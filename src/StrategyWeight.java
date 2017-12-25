import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Player;
import model.VehicleType;

public class StrategyWeight {
	
	private static final Map<String, Map<Vector2i, Double>> weightX1Map = new HashMap<>();
	private static final Map<String, Map<Vector2i, Double>> weightX2Map = new HashMap<>();
	
	public static void addGroupToWeightMapUpdates(String string) {
		Map<Vector2i, Double> weightX1CellMap = new HashMap<>();
		Map<Vector2i, Double> weightX2CellMap = new HashMap<>();
		for(Vector2i cellCoordinates : StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1).keySet()) {
			weightX1CellMap.put(cellCoordinates, 0.0);
		}
		for(Vector2i cellCoordinates : StrategyStorage.getGridByGridType(OperatingGroup.GridType.X2).keySet()) {
			weightX2CellMap.put(cellCoordinates, 0.0);
		}
		weightX1Map.put(string, weightX1CellMap);
		weightX2Map.put(string, weightX2CellMap);
	}
	
	public static void updateX2GridWeight(Player selfPlayer) {
		Map<Vector2i, ? extends StrategyGrid.StrategyCell> gridX2 = StrategyStorage.getGridByGridType(OperatingGroup.GridType.X2);
		for(String groupName : weightX2Map.keySet()) {
			for(Vector2i cellCoordinates : weightX2Map.get(groupName).keySet()) {
				weightX2Map.get(groupName).put(cellCoordinates, 0.0);
			}
		}
		for(Vector2i cellCoordinates : gridX2.keySet()) {
			StrategyGrid.StrategyCellX2 strategyCell = (StrategyGrid.StrategyCellX2) gridX2.get(cellCoordinates);
			for(StrategyGrid.StrategyCellX1 cellX1 : strategyCell.cellsX1) {
				for(String groupName : weightX2Map.keySet()) {
					int friendlyVehicleWeight = 0;
					int enemyVehicleWeight = 0;
					for(long friendlyVehicleId : StrategyStorage.operatingGroups.get(groupName).vehicleList) {
						ExtendedGameObjects.VehicleExtended extendedPlayerVehicle = StrategyStorage.vehicleExtendedById.get(friendlyVehicleId);
						friendlyVehicleWeight += extendedPlayerVehicle.getDurability();
					}
					for(ExtendedGameObjects.VehicleExtended extendedGroundVehicle : cellX1.groundVehiclesOnCell) {
						if(extendedGroundVehicle.vehicle.getPlayerId() == selfPlayer.getId()) {
							continue;
						}
						enemyVehicleWeight += extendedGroundVehicle.getDurability();
					}
					for(ExtendedGameObjects.VehicleExtended extendedFlightVehicle : cellX1.flightVehiclesOnCell) {
						if(extendedFlightVehicle.vehicle.getPlayerId() == selfPlayer.getId()) {
							continue;
						}
						enemyVehicleWeight += extendedFlightVehicle.getDurability();
					}
					if(enemyVehicleWeight != 0) {
						weightX2Map.get(groupName).put(cellCoordinates, weightX2Map.get(groupName).get(cellCoordinates) + (friendlyVehicleWeight - enemyVehicleWeight));
					}
					else {
						weightX2Map.get(groupName).put(cellCoordinates, 0.0);
					}
				}
			}
		}
		interpolateX2GridWeight();
	}
	
	private static void interpolateX2GridWeight() {
		for(String groupName : weightX2Map.keySet()) {
			for(Vector2i cellCoordinates : weightX2Map.get(groupName).keySet()) {
				double weight = weightX2Map.get(groupName).get(cellCoordinates);
				for(int x = -1; x <= 1; x++) {
					for(int y = -1; y <= 1; y++) {
						Vector2i aroundCheckCoordinates = cellCoordinates.add(x, y);
						if(weightX2Map.get(groupName).containsKey(aroundCheckCoordinates) && (x != 0 && y != 0)) {
							weight += (weightX2Map.get(groupName).get(aroundCheckCoordinates) / 4);
						}
					}
				}
				weightX2Map.get(groupName).put(cellCoordinates, weight);
			}
		}
	}
	
	public static double getCellWeight(String groupName, Vector2i cellCoordinates) {
		Map<String, Map<Vector2i, Double>> weightMap;
		if(StrategyStorage.operatingGroups.get(groupName).gridType == OperatingGroup.GridType.X1) {
			weightMap = weightX1Map;
		} else {
			weightMap = weightX2Map;
		}
		
		if(weightMap.containsKey(groupName) && weightMap.get(groupName).containsKey(cellCoordinates)) {
			return weightMap.get(groupName).get(cellCoordinates);
		}
		return 0;
	}
	
	public static Vector2i getGroupTarget(String groupName, Collection<Vector2i> usedCells) {
		Map<String, Map<Vector2i, Double>> weightMap;
		if(StrategyStorage.operatingGroups.get(groupName).gridType == OperatingGroup.GridType.X1) {
			weightMap = weightX1Map;
		} else {
			weightMap = weightX2Map;
		}
		Map<Vector2i, Double> map = weightMap.get(groupName);
		Vector2i targetCoordinates = new Vector2i();
		double weight = 0;
		for(Vector2i cellCoordinates : map.keySet()) {
			if((usedCells != null) && usedCells.contains(cellCoordinates)) {
				continue;
			}
			double calculatedWeight = map.get(cellCoordinates);
			if(calculatedWeight > weight) {
				weight = calculatedWeight;
				targetCoordinates.set(cellCoordinates);
			}
		}
		return targetCoordinates;
	}
	
	public static double getTargetWeight(List<ExtendedGameObjects.VehicleExtended> friendlyVehicles, List<ExtendedGameObjects.VehicleExtended> enemyVehicles) {
		double weight = 0;
		for(ExtendedGameObjects.VehicleExtended friendlyVehicle : friendlyVehicles) {
			for(ExtendedGameObjects.VehicleExtended enemyVehicle : enemyVehicles) {
				weight += calculateSingleVehicleWeight(friendlyVehicle, enemyVehicle);
			}
		}
		return weight;
	}
	
	private static double calculateSingleVehicleWeight(ExtendedGameObjects.VehicleExtended friendlyVehicle, ExtendedGameObjects.VehicleExtended enemyVehicle) {
		
		VehicleType friendlyType = friendlyVehicle.vehicle.getType();
		VehicleType enemyType = enemyVehicle.vehicle.getType();
		
		double friendlyDamage;
		double enemyDamage;
		double friendlyDefence;
		double enemyDefence;
		
		if((friendlyType == VehicleType.FIGHTER) || (friendlyType == VehicleType.HELICOPTER)) {
			enemyDamage = enemyVehicle.vehicle.getAerialDamage();
			enemyDefence = enemyVehicle.vehicle.getAerialDefence();
		} else {
			enemyDamage = enemyVehicle.vehicle.getGroundDamage();
			enemyDefence = enemyVehicle.vehicle.getGroundDefence();
		}
		if(enemyType == VehicleType.FIGHTER || enemyType == VehicleType.HELICOPTER) {
			friendlyDamage = friendlyVehicle.vehicle.getAerialDamage();
			friendlyDefence = friendlyVehicle.vehicle.getAerialDefence();
		} else {
			friendlyDamage = friendlyVehicle.vehicle.getGroundDamage();
			friendlyDefence = friendlyVehicle.vehicle.getGroundDefence();
		}
		
		double friendlyWeight = StrictMath.max(friendlyDamage - enemyDefence, 0) * friendlyVehicle.getDurability() / 100.0d;
		double enemyWeight = StrictMath.max(enemyDamage - friendlyDefence, 0) * enemyVehicle.getDurability() / 100.0d;
		
		return friendlyWeight - enemyWeight;
	}
}
