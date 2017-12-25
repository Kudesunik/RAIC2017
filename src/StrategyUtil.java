import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import model.Player;
import model.VehicleType;
import model.World;

public class StrategyUtil {
	
	private static short currentGroupId = 0;
	private static int currentActionId = 0;
	
	public static Map<Integer, Boolean> isActionExecutedMap = new HashMap<>();
	
	public static short generateNextGroupId(int maxUnitGroup) {
		if((++currentGroupId) < maxUnitGroup) {
			return currentGroupId;
		}
		return (short) maxUnitGroup;
	}
	
	public static int generateNextActionId() {
		isActionExecutedMap.put(++currentActionId, false);
		return currentActionId;
	}
	
	public static Stream<ExtendedGameObjects.VehicleExtended> streamVehiclesInstances(Player selfPlayer, ExtendedGameObjects.VehicleSide vehicleSide, VehicleType vehicleType) {
		
		Stream<ExtendedGameObjects.VehicleExtended> stream = StrategyStorage.vehicleExtendedById.values().stream();
		
		switch(vehicleSide) {
		case FRIEND:
			stream = stream.filter((ExtendedGameObjects.VehicleExtended vehicle) -> (vehicle.vehicle.getPlayerId() == selfPlayer.getId()));
			break;
		case ENEMY:
			stream = stream.filter((ExtendedGameObjects.VehicleExtended vehicle) -> (vehicle.vehicle.getPlayerId() != selfPlayer.getId()));
			break;
		default:
		}
		
		if(vehicleType != null) {
			stream = stream.filter((ExtendedGameObjects.VehicleExtended vehicle) -> (vehicle.vehicle.getType() == vehicleType));
		}
		
		return stream;
	}
	
	public static Vector2d[] getBoundsCoordinates(Player selfPlayer, World world, VehicleType vehicleType) {
		Vector2d min = new Vector2d(StrategyUtil.streamVehiclesInstances(selfPlayer, ExtendedGameObjects.VehicleSide.FRIEND, vehicleType).mapToDouble(ExtendedGameObjects.VehicleExtended::getX).min().orElse(0), StrategyUtil.streamVehiclesInstances(selfPlayer, ExtendedGameObjects.VehicleSide.FRIEND, vehicleType).mapToDouble(ExtendedGameObjects.VehicleExtended::getY).min().orElse(0));
		Vector2d max = new Vector2d(StrategyUtil.streamVehiclesInstances(selfPlayer, ExtendedGameObjects.VehicleSide.FRIEND, vehicleType).mapToDouble(ExtendedGameObjects.VehicleExtended::getX).max().orElse(world.getWidth()), StrategyUtil.streamVehiclesInstances(selfPlayer, ExtendedGameObjects.VehicleSide.FRIEND, vehicleType).mapToDouble(ExtendedGameObjects.VehicleExtended::getY).max().orElse(world.getHeight()));
		return new Vector2d[]{min, max};
	}
	
	public static Vector2d[] getBoundsCoordinates(List<Long> vehicleList) {
		Vector2d min = new Vector2d(vehicleList.stream().map((m) -> StrategyStorage.vehicleExtendedById.get(m.longValue())).mapToDouble(ExtendedGameObjects.VehicleExtended::getX).min().orElse(0), vehicleList.stream().map((m) -> StrategyStorage.vehicleExtendedById.get(m.longValue())).mapToDouble(ExtendedGameObjects.VehicleExtended::getY).min().orElse(0));
		Vector2d max = new Vector2d(vehicleList.stream().map((m) -> StrategyStorage.vehicleExtendedById.get(m.longValue())).mapToDouble(ExtendedGameObjects.VehicleExtended::getX).max().orElse(0), vehicleList.stream().map((m) -> StrategyStorage.vehicleExtendedById.get(m.longValue())).mapToDouble(ExtendedGameObjects.VehicleExtended::getY).max().orElse(0));
		return new Vector2d[]{min, max};
	}
	
	public static Vector2d getCenterCoordinatesByIdList(List<Long> vehicleList) {
		double x = vehicleList.stream().map((m) -> StrategyStorage.vehicleExtendedById.get(m.longValue())).mapToDouble(ExtendedGameObjects.VehicleExtended::getX).average().orElse(0);
		double y = vehicleList.stream().map((m) -> StrategyStorage.vehicleExtendedById.get(m.longValue())).mapToDouble(ExtendedGameObjects.VehicleExtended::getY).average().orElse(0);
		return new Vector2d(x, y);
	}
	
	public static Vector2d getCenterCoordinatesByVehicleList(List<ExtendedGameObjects.VehicleExtended> vehicleList) {
		return new Vector2d(vehicleList.stream().mapToDouble(ExtendedGameObjects.VehicleExtended::getX).average().orElse(0), vehicleList.stream().mapToDouble(ExtendedGameObjects.VehicleExtended::getY).average().orElse(0));
	}
	
	public static long getVehiceIdByClosestToCoordinates(Vector2d coordinates, List<Long> vehicleList) {
		double distance = Double.MAX_VALUE;
		long resultId = 0;
		for(long id : vehicleList) {
			ExtendedGameObjects.VehicleExtended ve = StrategyStorage.vehicleExtendedById.get(id);
			Vector2d vehicleCoordinates = new Vector2d(ve.getX(), ve.getY());
			double bufferDistance = vehicleCoordinates.distance(coordinates);
			if(vehicleCoordinates.distance(coordinates) < distance) {
				resultId = id;
				distance = bufferDistance;
			}
		}
		return resultId;
	}
	
	public static Vector2i getClosestCellByCoordinates(OperatingGroup group) {
		return getClosestCellByCoordinates(group, getCenterCoordinatesByIdList(group.vehicleList));
	}
	
	public static String findGroupNameById(int id) {
		for(String groupName : StrategyStorage.operatingGroups.keySet()) {
			if(StrategyStorage.operatingGroups.get(groupName).groupId == id) {
				return groupName;
			}
		}
		return null;
	}
	
	public static Vector2i getClosestCellByCoordinates(OperatingGroup group, Vector2d groupCenter) {
		Vector2i closestCellResult = new Vector2i();
		double distanceToCenter = Double.MAX_VALUE;
		for(Vector2i cellCoordinatesPoint : StrategyStorage.getGridByGridType(group.gridType).keySet()) {
			Vector2d cellRealCenter = StrategyStorage.getGridByGridType(group.gridType).get(cellCoordinatesPoint).getCellCenterCoordinates();
			double innerDistanceToCenter = cellRealCenter.distance(groupCenter);
			if(distanceToCenter > innerDistanceToCenter) {
				distanceToCenter = innerDistanceToCenter;
				closestCellResult.x = cellCoordinatesPoint.x;
				closestCellResult.y = cellCoordinatesPoint.y;
			}
		}
		return closestCellResult;
	}
	
	public static Vector2i getClosestCellByCoordinates(Vector2d coordinates, OperatingGroup.GridType gridType) {
		Vector2i closestCellResult = new Vector2i();
		double distanceToCenter = Double.MAX_VALUE;
		for(Vector2i cellCoordinatesPoint : StrategyStorage.getGridByGridType(gridType).keySet()) {
			Vector2d cellRealCenter = StrategyStorage.getGridByGridType(gridType).get(cellCoordinatesPoint).getCellCenterCoordinates();
			double innerDistanceToCenter = cellRealCenter.distance(coordinates);
			if(distanceToCenter > innerDistanceToCenter) {
				distanceToCenter = innerDistanceToCenter;
				closestCellResult.x = cellCoordinatesPoint.x;
				closestCellResult.y = cellCoordinatesPoint.y;
			}
		}
		return closestCellResult;
	}
	
	public static Vector2i getClosestCellToGroup(OperatingGroup operatingGroup, List<Vector2i> cellCoordinates) {
		Vector2i closestCellResult = new Vector2i();
		double distanceToCenter = Double.MAX_VALUE;
		for(Vector2i cellCoord : cellCoordinates) {
			Vector2i groupPosition = operatingGroup.getCurrentCell().getCellCoordinates();
			double innerDistanceToCenter = cellCoord.distance(groupPosition);
			if(distanceToCenter > innerDistanceToCenter) {
				distanceToCenter = innerDistanceToCenter;
				closestCellResult.x = cellCoord.x;
				closestCellResult.y = cellCoord.y;
			}
		}
		return closestCellResult;
	}
	
	/**
	 * Russian AI Cup 2017 (Code Wars contest)
	 * <br>Simple pathfinding class
	 * 
	 * @since 2017
	 * @author Kunik
	 */
	
	public static class StrategyPathfind {
		
		/**
		 * Strategy cell finder
		 * @param from (source cell coordinates)
		 * @param to (target cell coordinates)
		 * @return Free cell or null if no cell found
		 */
		public static StrategyGrid.StrategyCell getNextFreeStrategyCell(OperatingGroup operatingGroup, Vector2i to) {
			
			Map<Vector2i, ? extends StrategyGrid.StrategyCell> grid = StrategyStorage.getGridByGridType(operatingGroup.gridType);
			
			Vector2i from = operatingGroup.getCurrentCell().getCellCoordinates();
			
			int distanceX = to.x - from.x;
			int distanceY = to.y - from.y;
			
			int stepX = (int) Math.signum(distanceX);
			int stepY = (int) Math.signum(distanceY);
			
			Vector2i nextCellVectorX = from.add(stepX, 0);
			Vector2i nextCellVectorY = from.add(0, stepY);
			Vector2i outCellVectorX = from.add(0, stepX * 2);
			Vector2i outCellVectorY = from.add(stepY * 2, 0);
			Vector2i nextCellVectorDiagonal = from.add(stepX, stepY);
			
			if(!grid.containsKey(nextCellVectorDiagonal)) {
				return null;
			}
			
			StrategyGrid.StrategyCell nextCellX = grid.get(nextCellVectorX);
			StrategyGrid.StrategyCell nextCellY = grid.get(nextCellVectorY);
			StrategyGrid.StrategyCell outCellX = grid.get(outCellVectorX);
			StrategyGrid.StrategyCell outCellY = grid.get(outCellVectorY);
			StrategyGrid.StrategyCell nextCellDiagonal = grid.get(nextCellVectorDiagonal);
			
			if(!nextCellX.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId) && !nextCellY.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId) && !nextCellDiagonal.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId)) {
				return nextCellDiagonal;
			}
			
			if((stepX * stepY == 0) && (findGroupNameById(operatingGroup.groupId) != null) && (StrategyWeight.getCellWeight(findGroupNameById(operatingGroup.groupId), nextCellDiagonal.getCellCoordinates()) > 0) && !nextCellDiagonal.isFriendsExistsOnCell(StrategyStorage.playerId, StrategyGrid.CellVehicleType.MIXED)) {
				return nextCellDiagonal;
			}
			
			if(Math.abs(distanceX) > Math.abs(distanceY)) {
				if(!nextCellX.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId) && !nextCellVectorX.equals(from)) {
					return nextCellX;
				} else if(!nextCellY.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId) && !nextCellVectorY.equals(from)) {
					return nextCellY;
				}
			} else {
				if(!nextCellY.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId) && !nextCellVectorY.equals(from)) {
					return nextCellY;
				} else if(!nextCellX.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId) && !nextCellVectorX.equals(from)) {
					return nextCellX;
				}
			}
			
			if(Math.abs(distanceX) > Math.abs(distanceY)) {
				if((stepX != 0) && (outCellX != null) && !outCellX.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId)) {
					return outCellX;
				}
				if((stepY != 0) && (outCellY != null) && !outCellY.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId)) {
					return outCellY;
				}
			} else {
				if((stepY != 0) && (outCellY != null) && !outCellY.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId)) {
					return outCellY;
				}
				if((stepX != 0) && (outCellX != null) && !outCellX.isCellBusy(operatingGroup.cellVehicleType, operatingGroup.groupId)) {
					return outCellX;
				}
			}
			
			return null;
		}
	}
	
	/**
	 * Russian AI Cup 2017 (Code Wars contest)
	 * <br>Math class
	 * 
	 * @since 2017
	 * @author Kunik
	 */
	
	public static class StrategyMath {
		
		public static double roundToPlaces(double value, int places) {
			long factor = (long) Math.pow(10, places);
			value = value * factor;
			long buffer = Math.round(value);
			return (double) buffer / factor;
		}
	}
}
