import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.ActionType;
import model.FacilityType;
import model.VehicleType;

public class StrategyAI {
	
	private static final List<String> controllableGroups = new ArrayList<>();
	
	private static final Map<String, Vector2i> targetCells = new HashMap<>();
	
	public static void addGroupToAI(String groupName) {
		controllableGroups.add(groupName);
		targetCells.put(groupName, new Vector2i(1, 1));
		StrategyWeight.addGroupToWeightMapUpdates(groupName);
	}
	
	private final StrategyMoveController moveController;
	
	public StrategyAI(StrategyMoveController moveController) {
		this.moveController = moveController;
	}
	
	public void control(int currentTick) {
		for(String operatingGroupName : controllableGroups) {
			OperatingGroup operatingGroup = StrategyStorage.operatingGroups.get(operatingGroupName);
			if(operatingGroup.cellVehicleType == StrategyGrid.CellVehicleType.MIXED) {
				controlMainGroups(operatingGroupName, currentTick);
			}
			updateNuclear(operatingGroupName, currentTick);
		}
		updateFactories(currentTick);
	}
	
	private void controlMainGroups(String operatingGroupName, int currentTick) {
		OperatingGroup operatingGroup = StrategyStorage.operatingGroups.get(operatingGroupName);
		Vector2i target = null;
		List<Vector2i> usedCells = new ArrayList<Vector2i>(targetCells.values());
		usedCells.remove(targetCells.get(operatingGroupName));
		double lastDistance = Double.MAX_VALUE;
		for(long facilityId : StrategyStorage.facilityExtendedById.keySet()) {
			ExtendedGameObjects.FacilityExtended facilityExtended = StrategyStorage.facilityExtendedById.get(facilityId);
			Vector2i facilityCoordinates = facilityExtended.cell.getCellCoordinates();
			if((facilityExtended.getFacility().getOwnerPlayerId() != StrategyStorage.playerId) && !usedCells.contains(facilityCoordinates)) {
				double distance = facilityCoordinates.distance(operatingGroup.getCurrentCell().getCellCoordinates());
				if(distance < lastDistance) {
					target = facilityExtended.cell.getCellCoordinates();
					lastDistance = distance;
				}
			}
		}
		if(target == null) {
			target = StrategyWeight.getGroupTarget(operatingGroupName, usedCells);
		}
		if(targetCells.get(operatingGroupName).equals(target)) {
			return;
		}
		targetCells.get(operatingGroupName).set(target);
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.DROP, (strategyAction) -> {
			strategyAction.setOperatingGroup(operatingGroup);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(operatingGroup);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, targetCells.get(operatingGroupName));
			strategyAction.setBetweenActionDelay(60);
		});
	}
	
	private void updateFactories(int currentTick) {
		for(long facilityId : StrategyStorage.facilityExtendedById.keySet()) {
			ExtendedGameObjects.FacilityExtended factory = StrategyStorage.facilityExtendedById.get(facilityId);
			if(factory.getFacility().getType() == FacilityType.VEHICLE_FACTORY && factory.getFacility().getOwnerPlayerId() == StrategyStorage.playerId) {
				if((factory.stopProductionTick + 500 < currentTick) && !factory.cell.isCellBusy(StrategyGrid.CellVehicleType.MIXED, 0)) {
					if(factory.getFacility().getVehicleType() == null) {
						moveController.addActionToQueue((move) -> {
							move.setAction(ActionType.SETUP_VEHICLE_PRODUCTION);
							move.setFacilityId(facilityId);
							move.setVehicleType(VehicleType.TANK);
						});
						factory.startProductionTick = currentTick;
					}
				}
				if(factory.getFacility().getVehicleType() == VehicleType.TANK && currentTick > factory.startProductionTick + 2000 && currentTick < factory.startProductionTick + 4000) {
					moveController.addActionToQueue((move) -> {
						move.setAction(ActionType.SETUP_VEHICLE_PRODUCTION);
						move.setFacilityId(facilityId);
						move.setVehicleType(VehicleType.HELICOPTER);
					});
				}
			}
			if((factory.startProductionTick != 0) && ((factory.startProductionTick != -1)) && (factory.startProductionTick + 4000) < currentTick) {
				moveController.addActionToQueue((move) -> {
					move.setAction(ActionType.SETUP_VEHICLE_PRODUCTION);
					move.setFacilityId(facilityId);
				});
				moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.DROP, (strategyAction) -> {
					strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("Mixed-1"));
				});
				moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.CUSTOM, (strategyAction) -> {
					strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("Mixed-1"));
					StrategyMoveController.StrategyAction.IStrategyCustomAction customAction = () -> {
						OperatingGroup group = OperatingGroup.createGroupX2FromX2Cell(moveController, factory.cell.getCellCoordinates());
						StrategyStorage.operatingGroups.put("Mixed-" + group.groupId, group);
						StrategyAI.addGroupToAI("Mixed-" + group.groupId);
						StrategyUtil.isActionExecutedMap.put(strategyAction.getActionId(), true);
					};
					strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CUSTOM_METHOD, customAction);
				});
				factory.stopProductionTick = currentTick;
				factory.startProductionTick = 0;
			}
		}
	}
	
	public void updateNuclear(String operatingGroupName, int currentTick) {
		OperatingGroup operatingGroup = StrategyStorage.operatingGroups.get(operatingGroupName);
		StrategyGrid.StrategyCellX2 groupCellX2 = (StrategyGrid.StrategyCellX2) operatingGroup.getCurrentCell();
		StrategyGrid.StrategyCellX1 foundedEnemyCell = null;
		ExtendedGameObjects.VehicleExtended foundedFriendlyAimerVehicle = null;
		double size = 0;
		for(StrategyGrid.StrategyCellX1 friendlyCellX1 : groupCellX2.cellsX1) {
			List<ExtendedGameObjects.VehicleExtended> friendlyVehiclesOnCell = friendlyCellX1.groundVehiclesOnCell;
			if(friendlyVehiclesOnCell.isEmpty()) {
				friendlyVehiclesOnCell = friendlyCellX1.flightVehiclesOnCell;
			}
			Vector2d formationCenter = StrategyUtil.getCenterCoordinatesByVehicleList(friendlyVehiclesOnCell);
			ExtendedGameObjects.VehicleExtended friendlyAimerVehicle = StrategyStorage.vehicleExtendedById.get(StrategyUtil.getVehiceIdByClosestToCoordinates(formationCenter, operatingGroup.vehicleList));
			if(friendlyAimerVehicle == null) {
				continue;
			}
			Vector2i friendlyAimerVehicleCellCoordinates = StrategyUtil.getClosestCellByCoordinates(new Vector2d(friendlyAimerVehicle.getX(), friendlyAimerVehicle.getY()), OperatingGroup.GridType.X1);
			for(int x = -3; x <= 3; x++) {
				for(int y = -3; y <= 3; y++) {
					if(x == 0 && y == 0) {
						continue;
					}
					Vector2i checkCellCoordinates = friendlyAimerVehicleCellCoordinates.add(x, y);
					if(!StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1).containsKey(checkCellCoordinates)) {
						continue;
					}
					StrategyGrid.StrategyCellX1 enemyCell = (StrategyGrid.StrategyCellX1) StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1).get(checkCellCoordinates);
					List<ExtendedGameObjects.VehicleExtended> enemyVehicles = new ArrayList<>();
					for(ExtendedGameObjects.VehicleExtended enemyFlightVehicle : enemyCell.flightVehiclesOnCell) {
						enemyVehicles.add(enemyFlightVehicle);
					}
					for(ExtendedGameObjects.VehicleExtended enemyGroundVehicle : enemyCell.groundVehiclesOnCell) {
						enemyVehicles.add(enemyGroundVehicle);
					}
					if((enemyVehicles.size() == 1 || enemyVehicles.size() > 5) && (friendlyAimerVehicle != null) && !enemyCell.isFriendsExistsOnCell(StrategyStorage.playerId, StrategyGrid.CellVehicleType.MIXED) && enemyVehicles.size() > size) {
						Vector2d deployCenter = enemyCell.getCellCenterCoordinates();
						double distance = deployCenter.distance(new Vector2d(friendlyAimerVehicle.getX(), friendlyAimerVehicle.getY()));
						if(friendlyAimerVehicle.vehicle.getType() == VehicleType.FIGHTER && distance > 80) {
							continue;
						}
						if(friendlyAimerVehicle.vehicle.getType() == VehicleType.HELICOPTER && distance > 80) {
							continue;
						}
						if(friendlyAimerVehicle.vehicle.getType() == VehicleType.TANK && distance > 80) {
							continue;
						}
						if(friendlyAimerVehicle.vehicle.getType() == VehicleType.IFV && distance > 60) {
							continue;
						}
						if(friendlyAimerVehicle.vehicle.getType() == VehicleType.ARRV && distance > 60) {
							continue;
						}
						size = enemyVehicles.size();
						foundedEnemyCell = enemyCell;
						foundedFriendlyAimerVehicle = friendlyAimerVehicle;
					}
				}
			}
		}
		StrategyGrid.StrategyCellX1 foundedEnemyCellFinal = foundedEnemyCell;
		ExtendedGameObjects.VehicleExtended foundedFriendlyAimerVehicleFinal = foundedFriendlyAimerVehicle;
		if(size != 0 && foundedEnemyCell != null && foundedFriendlyAimerVehicle != null && StrategyStorage.canDeployNuclear) {
			Vector2d deployCenter = foundedEnemyCellFinal.getCellCenterCoordinates();
			moveController.addActionToQueue((move) -> {
				move.setAction(ActionType.TACTICAL_NUCLEAR_STRIKE);
				move.setVehicleId(foundedFriendlyAimerVehicleFinal.id);
				move.setX(deployCenter.x);
				move.setY(deployCenter.y);
			});
		}
	}
}
