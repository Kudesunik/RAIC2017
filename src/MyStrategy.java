import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import model.Facility;
import model.Game;
import model.Move;
import model.Player;
import model.Vehicle;
import model.VehicleType;
import model.VehicleUpdate;
import model.World;

public final class MyStrategy implements Strategy {
	
	/**
	 * Game strategy for CodeWars contest
	 * Russian AI Cup 2017
	 * 
	 * @since 2017
	 * @author Kunik
	 */
	
	private World world;
	private Game game;
	
	private boolean isFirstLaunch;
	
	private StrategyMoveController moveController;
	
	private final StrategyAI ai;
	
	public MyStrategy() {
		this.isFirstLaunch = true;
		this.moveController = new StrategyMoveController();
		this.ai = new StrategyAI(moveController);
	}
	
	@Override
	public void move(Player selfPlayer, World world, Game game, Move move) {
		
		this.world = world;
		this.game = game;
		
		if(isFirstLaunch) {
			StrategyStorage.playerId = selfPlayer.getId();
			StrategyStorage.maxUnitGroup = game.getMaxUnitGroup();
			StrategyGrid.setupStrategyGrid(world);
		}
		
		updateGameObjects();
		
		updateCellStatus();
		
		if(world.getTickIndex() % 10 == 0) {
			StrategyWeight.updateX2GridWeight(selfPlayer);
		}
		
		if(selfPlayer.getRemainingActionCooldownTicks() > 0) {
			return;
		}
		
		 if(isFirstLaunch) {
			 
			StrategyStorage.operatingGroups.put("TankGroup", OperatingGroup.createInitialOperatingGroup(selfPlayer, game, world, moveController, VehicleType.TANK));
			StrategyStorage.operatingGroups.put("IfvGroup", OperatingGroup.createInitialOperatingGroup(selfPlayer, game, world, moveController, VehicleType.IFV));
			StrategyStorage.operatingGroups.put("ArrvGroup", OperatingGroup.createInitialOperatingGroup(selfPlayer, game, world, moveController, VehicleType.ARRV));
			StrategyStorage.operatingGroups.put("HelicopterGroup", OperatingGroup.createInitialOperatingGroup(selfPlayer, game, world, moveController, VehicleType.HELICOPTER));
			StrategyStorage.operatingGroups.put("FighterGroup", OperatingGroup.createInitialOperatingGroup(selfPlayer, game, world, moveController, VehicleType.FIGHTER));
			
			setupGameStrategy(moveController);
			
			isFirstLaunch = false;
		}
		
		if(selfPlayer.getRemainingNuclearStrikeCooldownTicks() != 0) {
			StrategyStorage.canDeployNuclear = false;
		} else {
			StrategyStorage.canDeployNuclear = true;
		}
		
		if(moveController.executeQueueAction(move)) {
			return;
		}
		
		moveController.executeActionDequeActions(world.getTickIndex());
		
		if(world.getTickIndex() % 20 == 0) {
			ai.control(world.getTickIndex());
		}
	}
	
	private void setupGameStrategy(StrategyMoveController moveController) {
		
		List<Vector2i> groundCellCoordinates = new ArrayList<>();
		
		Vector2i collectionPoint = new Vector2i(2, 2);
		
		groundCellCoordinates.add(collectionPoint);
		groundCellCoordinates.add(new Vector2i(2, 6));
		groundCellCoordinates.add(new Vector2i(6, 2));
		
		List<Vector2i> closestCells = new LinkedList<>();
		List<Vector2d> sideBeforeShifts = new LinkedList<>();
		List<Vector2d> sideAfterShifts = new LinkedList<>();
		List<Vector2d> sandwichShifts = new LinkedList<>();
		
		closestCells.add(StrategyUtil.getClosestCellToGroup(StrategyStorage.operatingGroups.get("TankGroup"), groundCellCoordinates));
		groundCellCoordinates.remove(closestCells.get(0));
		closestCells.add(StrategyUtil.getClosestCellToGroup(StrategyStorage.operatingGroups.get("IfvGroup"), groundCellCoordinates));
		groundCellCoordinates.remove(closestCells.get(1));
		closestCells.add(StrategyUtil.getClosestCellToGroup(StrategyStorage.operatingGroups.get("ArrvGroup"), groundCellCoordinates));
		groundCellCoordinates.clear();
		
		for(int i = 0; i < closestCells.size(); i++) {
			Vector2i closestCell = closestCells.get(i);
			if(closestCell.x < closestCell.y) {
				sideBeforeShifts.add(new Vector2d(game.getVehicleRadius() * 3.0, 0));
				sideAfterShifts.add(new Vector2d(-game.getVehicleRadius(), 0));
			} else if(closestCell.x > closestCell.y) {
				sideBeforeShifts.add(new Vector2d(0, game.getVehicleRadius() * 3.0));
				sideAfterShifts.add(new Vector2d(game.getVehicleRadius() * 4.2, 0));
			} else {
				sideBeforeShifts.add(new Vector2d(0, 0));
				sideAfterShifts.add(new Vector2d(0, 0));
			}
		}
		
		for(int i = 0; i < closestCells.size(); i++) {
			Vector2i closestCell = closestCells.get(i);
			if(closestCell.x < closestCell.y) {
				sandwichShifts.add(new Vector2d(0, -StrategyGrid.cellStepY * 4));
			} else if(closestCell.x > closestCell.y) {
				sandwichShifts.add(new Vector2d(-StrategyGrid.cellStepX * 4, 0));
			} else {
				sandwichShifts.add(new Vector2d(0, 0));
			}
		}
		
		List<OperatingGroup> mainGroups = new LinkedList<>();
		
		mainGroups.add(StrategyStorage.operatingGroups.get("TankGroup"));
		mainGroups.add(StrategyStorage.operatingGroups.get("IfvGroup"));
		mainGroups.add(StrategyStorage.operatingGroups.get("ArrvGroup"));
		
		int tankId = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("TankGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(0));
			strategyAction.setBetweenActionDelay(120);
			strategyAction.setAfterActionDelay(120);
		});
		int ifvId = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("IfvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(1));
			strategyAction.setBetweenActionDelay(60);
			strategyAction.setAfterActionDelay(60);
		});
		int arrvId = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("ArrvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(2));
			strategyAction.setBetweenActionDelay(60);
			strategyAction.setAfterActionDelay(60);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("FighterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(0));
			strategyAction.setBetweenActionDelay(30);
			strategyAction.setAfterActionDelay(30);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("HelicopterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(1));
			strategyAction.setBetweenActionDelay(30);
			strategyAction.setAfterActionDelay(30);
		});
		
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("TankGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 2.12);
			strategyAction.waitUntilActionIds(tankId, ifvId, arrvId);
			strategyAction.setAfterActionDelay(240);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("IfvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 2.12);
			strategyAction.waitUntilActionIds(tankId, ifvId, arrvId);
			strategyAction.setAfterActionDelay(120);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("ArrvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 2.12);
			strategyAction.waitUntilActionIds(tankId, ifvId, arrvId);
			strategyAction.setAfterActionDelay(120);
		});
		
		int helicopterExpandId = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("HelicopterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 2.12);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(1));
			strategyAction.waitUntilActionIds(tankId, ifvId, arrvId);
			strategyAction.setAfterActionDelay(180);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("FighterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 2.12);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, closestCells.get(0));
			strategyAction.waitUntilActionIds(helicopterExpandId);
			strategyAction.setAfterActionDelay(180);
		});
		
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("TankGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sideBeforeShifts.get(0));
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("IfvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sideBeforeShifts.get(1));
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("ArrvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sideBeforeShifts.get(2));
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("FighterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sideBeforeShifts.get(0));
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("HelicopterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sideBeforeShifts.get(1));
		});
		
		int shiftId1 = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("TankGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sandwichShifts.get(0));
			strategyAction.setAfterActionDelay(180);
		});
		
		int shiftId2 = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("IfvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sandwichShifts.get(1));
			strategyAction.setAfterActionDelay(180);
			if(sandwichShifts.get(1).x + sandwichShifts.get(1).y != 0) {
				if(sandwichShifts.get(0).x + sandwichShifts.get(0).y != 0) {
					strategyAction.waitUntilActionIds(shiftId1);
				} else if(sandwichShifts.get(2).x + sandwichShifts.get(2).y != 0) {
					strategyAction.waitUntilActionIds(shiftId1 + 2);
				}
			}
		});
		
		int shiftId3 = moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("ArrvGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sandwichShifts.get(2));
			strategyAction.setAfterActionDelay(180);
			if(sandwichShifts.get(2).x + sandwichShifts.get(2).y != 0 && sandwichShifts.get(1).x + sandwichShifts.get(1).y == 0) {
				strategyAction.waitUntilActionIds(shiftId1);
			}
		});
		
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("FighterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sandwichShifts.get(0));
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.SHIFT, (strategyAction) -> {
			strategyAction.setOperatingGroup(StrategyStorage.operatingGroups.get("HelicopterGroup"));
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXACT_COORDINATES, sandwichShifts.get(1));
			strategyAction.waitUntilActionIds(shiftId1);
		});
		
		int groupCounter = 0;
		
		for(int x = 0; x <= 2; x += 2) {
			for(int y = 0; y <= 2; y += 2) {
				int xFinal = x;
				int yFinal = y;
				int groupCounterFinal = ++groupCounter;
				moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.CUSTOM, (strategyAction) -> {
					strategyAction.setOperatingGroup(mainGroups.get(1));
					StrategyMoveController.StrategyAction.IStrategyCustomAction customAction = () -> {
						OperatingGroup group = OperatingGroup.createGroupX2FromX2Cell(moveController, new Vector2i(1 + xFinal, 1 + yFinal));
						StrategyStorage.operatingGroups.put("Mixed-" + groupCounterFinal, group);
						StrategyAI.addGroupToAI("Mixed-" + groupCounterFinal);
						StrategyUtil.isActionExecutedMap.put(strategyAction.getActionId(), true);
					};
					strategyAction.waitUntilActionIds(shiftId1, shiftId2, shiftId3);
					strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CUSTOM_METHOD, customAction);
				});
			}
		}
	}
	
	private void updateGameObjects() {
		for(Vehicle vehicle : world.getNewVehicles()) {
			long id = vehicle.getId();
			StrategyStorage.vehicleExtendedById.put(id, new ExtendedGameObjects.VehicleExtended(id, vehicle));
		}
		for(Facility facility : world.getFacilities()) {
			if(StrategyStorage.facilityExtendedById.containsKey(facility.getId())) {
				StrategyStorage.facilityExtendedById.get(facility.getId()).setFacility(facility);
			}
			else {
				ExtendedGameObjects.FacilityExtended facilityExtended = new ExtendedGameObjects.FacilityExtended(facility.getId(), facility);
				Vector2d realCoordinates = new Vector2d(facility.getLeft() + StrategyGrid.cellStepX, facility.getTop() + StrategyGrid.cellStepY);
				facilityExtended.cell = (StrategyGrid.StrategyCellX2) StrategyStorage.getGridByGridType(OperatingGroup.GridType.X2).get(StrategyUtil.getClosestCellByCoordinates(realCoordinates, OperatingGroup.GridType.X2));
				StrategyStorage.facilityExtendedById.put(facility.getId(), facilityExtended);
			}
		}
		for(VehicleUpdate vehicleUpdate : world.getVehicleUpdates()) {
			if(vehicleUpdate.getDurability() == 0) {
				StrategyStorage.vehicleExtendedById.remove(vehicleUpdate.getId());
				for(OperatingGroup operatingGroup : StrategyStorage.operatingGroups.values()) {
					operatingGroup.vehicleList.remove(vehicleUpdate.getId());
				}
			} else {
				ExtendedGameObjects.VehicleExtended vehicleExtended = StrategyStorage.vehicleExtendedById.get(vehicleUpdate.getId());
				vehicleExtended.updatePosition(vehicleUpdate.getX(), vehicleUpdate.getY());
				vehicleExtended.updateDurability(vehicleUpdate.getDurability());
			}
		}
	}
	
	private void updateCellStatus() {
		if(!isFirstLaunch) {
			Collection<? extends StrategyGrid.StrategyCell> strategyCellsX1 = StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1).values();
			for(StrategyGrid.StrategyCell strategyCell : strategyCellsX1) {
				StrategyGrid.StrategyCellX1 strategyCellX1 = (StrategyGrid.StrategyCellX1) strategyCell;
				strategyCellX1.groundVehiclesOnCell.clear();
				strategyCellX1.flightVehiclesOnCell.clear();
			}
			for(ExtendedGameObjects.VehicleExtended vehicleExtended : StrategyStorage.vehicleExtendedById.values()) {
				StrategyGrid.StrategyCellX1 strategyCellX1 = StrategyGrid.getStrategyCellX1ByCoordinates(vehicleExtended.getX(), vehicleExtended.getY());
				if((vehicleExtended.vehicle.getType() == VehicleType.FIGHTER) || (vehicleExtended.vehicle.getType() == VehicleType.HELICOPTER)) {
					strategyCellX1.flightVehiclesOnCell.add(vehicleExtended);
				} else {
					strategyCellX1.groundVehiclesOnCell.add(vehicleExtended);
				}
			}
			for(StrategyGrid.StrategyCell strategyCell : StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1).values()) {
				StrategyGrid.StrategyCellX1 strategyCellX1 = (StrategyGrid.StrategyCellX1) strategyCell;
				strategyCellX1.checkAndRemoveOperatingGroupInCell();
			}
		}
	}
}
