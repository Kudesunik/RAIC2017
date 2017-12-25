import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import model.Game;
import model.Player;
import model.VehicleType;
import model.World;

public class OperatingGroup {
	
	public int groupId;
	
	public GridType gridType;
	public StrategyGrid.CellVehicleType cellVehicleType;
	public MoveStatus moveStatus;
	
	private StrategyGrid.StrategyCell strategyCell;
	
	public List<Long> vehicleList;
	
	public final List<Vector2i> path;
	
	public double currentExpandFactor;
	
	public double movingShiftX;
	public double movingShiftY;
	
	public int movementTickPause;
	public int movementTickDelay;
	
	public boolean isPositioned;
	
	private OperatingGroup(GridType gridType, StrategyGrid.CellVehicleType cellVehicleType) {
		this.gridType = gridType;
		this.cellVehicleType = cellVehicleType;
		this.vehicleList = new ArrayList<>();
		this.path = new LinkedList<>();
		this.moveStatus = MoveStatus.STAY;
		this.currentExpandFactor = 1.0;
		this.movementTickPause = 0;
		this.movementTickDelay = 0;
		this.isPositioned = false;
	}
	
	public void setOperatingGroupInCell(Vector2i cellCoordinates) {
		strategyCell = StrategyStorage.getGridByGridType(gridType).get(cellCoordinates);
		strategyCell.setOperatingGroupInCell(this);
	}
	
	public StrategyGrid.StrategyCell getCurrentCell() {
		if(strategyCell == null) {
			strategyCell = StrategyStorage.getGridByGridType(gridType).get(StrategyUtil.getClosestCellByCoordinates(this));
		}
		return strategyCell;
	}
	
	public boolean isGroupInCell(StrategyGrid.StrategyCell cell) {
		for(long id : vehicleList) {
			ExtendedGameObjects.VehicleExtended vehicleExtended = StrategyStorage.vehicleExtendedById.get(id);
			if(!cell.getCellRectangle().contains(vehicleExtended.getX(), vehicleExtended.getY())) {
				return false;
			}
		}
		return true;
	}
	
	public double getGroupMaxSpeed() {
		double maxSpeed = Double.MAX_VALUE;
		for(long id : vehicleList) {
			double maxSpeedInner = StrategyStorage.vehicleExtendedById.get(id).vehicle.getMaxSpeed();
			if(maxSpeed > maxSpeedInner) {
				maxSpeed = maxSpeedInner;
			}
		}
		return maxSpeed;
	}
	
	public static OperatingGroup createInitialOperatingGroup(Player selfPlayer, Game game, World world, StrategyMoveController moveController, VehicleType vehicleType) {
		
		StrategyGrid.CellVehicleType cellVehicleType = StrategyGrid.CellVehicleType.GROUND;
		
		if((vehicleType == VehicleType.FIGHTER) || (vehicleType == VehicleType.HELICOPTER)) {
			cellVehicleType = StrategyGrid.CellVehicleType.FLIGHT;
		}
		
		OperatingGroup operatingGroup = new OperatingGroup(GridType.X2, cellVehicleType);
		
		Vector2d[] groupBounds = StrategyUtil.getBoundsCoordinates(selfPlayer, world, vehicleType);
		
		StrategyUtil.streamVehiclesInstances(selfPlayer, ExtendedGameObjects.VehicleSide.FRIEND, vehicleType).filter((ExtendedGameObjects.VehicleExtended vehicle) -> {
			double x = vehicle.getX();
			double y = vehicle.getY();
			return (x >= groupBounds[0].x) && (x <= groupBounds[1].x) && (y >= groupBounds[0].y) && (y <= groupBounds[1].y);
		}).allMatch((ExtendedGameObjects.VehicleExtended vehicle) -> operatingGroup.vehicleList.add(vehicle.vehicle.getId()));
		
		operatingGroup.groupId = StrategyUtil.generateNextGroupId(game.getMaxUnitGroup());
		
		moveController.assignGroupToOperatingGroup(operatingGroup, null);
		
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (StrategyMoveController.StrategyAction.IStrategyAction strategyAction) -> {
			strategyAction.setOperatingGroup(operatingGroup);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, StrategyUtil.getClosestCellByCoordinates(operatingGroup));
			strategyAction.setAfterActionDelay(40);
		});
		
		return operatingGroup;
	}
	
	public static List<OperatingGroup> createGroupsX1FromX2Rectangle(Player selfPlayer, Game game, StrategyMoveController moveController, Rectangle cellX2Rectangle, VehicleType vehicleType) {
		List<OperatingGroup> operatingGroups = new ArrayList<>();
		StrategyGrid.StrategyCellX1 initialCell = StrategyGrid.getStrategyCellX1ByCoordinates(cellX2Rectangle.x + 2, cellX2Rectangle.y + 2);
		OperatingGroup groundGroup = initialCell.getOperatingGroupInCell(StrategyGrid.CellVehicleType.GROUND);
		OperatingGroup flightGroup = initialCell.getOperatingGroupInCell(StrategyGrid.CellVehicleType.FLIGHT);
		StrategyGrid.CellVehicleType cellVehicleType = null;
		if(groundGroup != null && flightGroup != null && vehicleType == null) {
			cellVehicleType = StrategyGrid.CellVehicleType.MIXED;
		}
		else if(groundGroup != null && vehicleType != VehicleType.FIGHTER && vehicleType != VehicleType.HELICOPTER) {
			cellVehicleType = StrategyGrid.CellVehicleType.GROUND;
		}
		else if(flightGroup != null && (vehicleType == VehicleType.FIGHTER || vehicleType == VehicleType.HELICOPTER)) {
			cellVehicleType = StrategyGrid.CellVehicleType.FLIGHT;
		}
		if(cellVehicleType == null) {
			return operatingGroups;
		}
		for(int xFactor = 1; xFactor <= 2; xFactor++) {
			for(int yFactor = 1; yFactor <= 2; yFactor++) {
				StrategyGrid.StrategyCellX1 cell = StrategyGrid.getStrategyCellX1ByCoordinates(cellX2Rectangle.x + StrategyGrid.cellStepX / 2 * xFactor, cellX2Rectangle.y + StrategyGrid.cellStepY / 2 * yFactor);
				OperatingGroup operatingGroup = new OperatingGroup(GridType.X1, cellVehicleType);
				cell.groundVehiclesOnCell.stream().mapToLong(ExtendedGameObjects.VehicleExtended::getId).filter((long id) -> vehicleType != null ? (StrategyStorage.vehicleExtendedById.get(id).vehicle.getType() == vehicleType) : true).forEach((long l) -> operatingGroup.vehicleList.add(l));
				cell.flightVehiclesOnCell.stream().mapToLong(ExtendedGameObjects.VehicleExtended::getId).filter((long id) -> vehicleType != null ? (StrategyStorage.vehicleExtendedById.get(id).vehicle.getType() == vehicleType) : true).forEach((long l) -> operatingGroup.vehicleList.add(l));
				operatingGroup.groupId = StrategyUtil.generateNextGroupId(game.getMaxUnitGroup());
				moveController.assignGroupToOperatingGroup(operatingGroup, vehicleType);
				moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (StrategyMoveController.StrategyAction.IStrategyAction strategyAction) -> {
					strategyAction.setOperatingGroup(operatingGroup);
					strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, StrategyUtil.getClosestCellByCoordinates(operatingGroup));
				});
				moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (StrategyMoveController.StrategyAction.IStrategyAction strategyAction) -> {
					strategyAction.setOperatingGroup(operatingGroup);
					strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 0.8);
					operatingGroups.add(operatingGroup);
				});
			}
		}
		return operatingGroups;
	}
	
	public static OperatingGroup createGroupX2FromX2Cell(StrategyMoveController moveController, Vector2i cellX2) {
		StrategyGrid.StrategyCellX2 cell = (StrategyGrid.StrategyCellX2) StrategyStorage.getGridByGridType(GridType.X2).get(cellX2);
		StrategyGrid.CellVehicleType cellVehicleType = StrategyGrid.CellVehicleType.MIXED;
		OperatingGroup operatingGroup = new OperatingGroup(GridType.X2, cellVehicleType);
		operatingGroup.vehicleList.addAll(cell.getGroundVehiclesOnCell());
		operatingGroup.vehicleList.addAll(cell.getFlightVehiclesOnCell());
		operatingGroup.groupId = StrategyUtil.generateNextGroupId(StrategyStorage.maxUnitGroup);
		moveController.assignGroupToOperatingGroup(operatingGroup, null);
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (strategyAction) -> {
			strategyAction.setOperatingGroup(operatingGroup);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.MAX_SPEED, 0.2);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, StrategyUtil.getClosestCellByCoordinates(operatingGroup));
			strategyAction.setAfterActionDelay(30);
		});
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
			strategyAction.setOperatingGroup(operatingGroup);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 0.70);
			strategyAction.setAfterActionDelay(30);
		});
		return operatingGroup;
	}
	
	public static OperatingGroup createGroupX1FromX1Cell(StrategyMoveController moveController, Vector2i cellCoordinates, VehicleType vehicleType) {
		StrategyGrid.StrategyCellX1 cell = (StrategyGrid.StrategyCellX1) StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1).get(cellCoordinates);
		OperatingGroup groundGroup = cell.getOperatingGroupInCell(StrategyGrid.CellVehicleType.GROUND);
		OperatingGroup flightGroup = cell.getOperatingGroupInCell(StrategyGrid.CellVehicleType.FLIGHT);
		StrategyGrid.CellVehicleType cellVehicleType = null;
		if(groundGroup != null && flightGroup != null && vehicleType == null) {
			cellVehicleType = StrategyGrid.CellVehicleType.MIXED;
		}
		else if(groundGroup != null && vehicleType != VehicleType.FIGHTER && vehicleType != VehicleType.HELICOPTER) {
			cellVehicleType = StrategyGrid.CellVehicleType.GROUND;
		}
		else if(flightGroup != null && (vehicleType == VehicleType.FIGHTER || vehicleType == VehicleType.HELICOPTER)) {
			cellVehicleType = StrategyGrid.CellVehicleType.FLIGHT;
		}
		OperatingGroup operatingGroup = new OperatingGroup(GridType.X1, cellVehicleType);
		cell.groundVehiclesOnCell.stream().mapToLong(ExtendedGameObjects.VehicleExtended::getId).filter((long id) -> vehicleType != null ? (StrategyStorage.vehicleExtendedById.get(id).vehicle.getType() == vehicleType) : true).forEach((long l) -> operatingGroup.vehicleList.add(l));
		cell.flightVehiclesOnCell.stream().mapToLong(ExtendedGameObjects.VehicleExtended::getId).filter((long id) -> vehicleType != null ? (StrategyStorage.vehicleExtendedById.get(id).vehicle.getType() == vehicleType) : true).forEach((long l) -> operatingGroup.vehicleList.add(l));
		operatingGroup.groupId = StrategyUtil.generateNextGroupId(StrategyStorage.maxUnitGroup);
		moveController.assignGroupToOperatingGroup(operatingGroup, vehicleType);
		moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.MOVE, (StrategyMoveController.StrategyAction.IStrategyAction strategyAction) -> {
			strategyAction.setOperatingGroup(operatingGroup);
			strategyAction.addActionParameter(StrategyMoveController.StrategyAction.CELL_COORDINATES, StrategyUtil.getClosestCellByCoordinates(operatingGroup));
		});
		return operatingGroup;
	}
	
	public enum GridType {
		X1, X2;
	}

	public enum MoveStatus {
		STAY, MOVING, EXPANDING;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + groupId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		OperatingGroup other = (OperatingGroup) obj;
		if(groupId != other.groupId)
			return false;
		return true;
	}
}
