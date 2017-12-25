import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

import model.ActionType;
import model.Move;
import model.VehicleType;

public class StrategyMoveController {
	
	private final Deque<Integer> groupIdDeque;
	
	private final Map<Integer, Deque<StrategyMoveController.StrategyAction.IStrategyAction>> strategyActionDeque;
	
	private final Deque<Consumer<Move>> actionQueue;
	
	public StrategyMoveController() {
		this.groupIdDeque = new LinkedBlockingDeque<>();
		this.actionQueue = new ArrayDeque<>();
		this.strategyActionDeque = new ConcurrentHashMap<>();
	}
	
	public void assignGroupToOperatingGroup(OperatingGroup group, VehicleType vehicleType) {
		Vector2d[] groupBounds = StrategyUtil.getBoundsCoordinates(group.vehicleList);
		addActionToQueue((Move move) -> {
			move.setAction(ActionType.CLEAR_AND_SELECT);
			if(vehicleType != null) {
				move.setVehicleType(vehicleType);
			}
			move.setLeft(groupBounds[0].x);
			move.setRight(groupBounds[1].x);
			move.setTop(groupBounds[0].y);
			move.setBottom(groupBounds[1].y);
		});
		addActionToQueue((Move move) -> {
			move.setAction(ActionType.ASSIGN);
			move.setGroup(group.groupId);
		});
		for(long vehicleId : group.vehicleList) {
			StrategyStorage.vehicleExtendedById.get(vehicleId).assignGroup(group.groupId);
		}
	}
	
	public static Vector2d getRealMove(Vector2d from, Vector2d to) {
		return to.subtract(from);
	}
	
	public void addActionToQueue(Consumer<Move> consumer) {
		actionQueue.add(consumer);
	}
	
	public int addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType actionType, Consumer<StrategyMoveController.StrategyAction.IStrategyAction> consumer) {
		StrategyMoveController.StrategyAction.IStrategyAction strategyAction = null;
		switch(actionType) {
		case MOVE:
			strategyAction = new StrategyMoveController.StrategyAction.StrategyMoveAction();
			break;
		case SHIFT:
			strategyAction = new StrategyMoveController.StrategyAction.StrategyShiftAction();
			break;
		case EXPAND:
			strategyAction = new StrategyMoveController.StrategyAction.StrategyExpandAction();
			break;
		case CUSTOM:
			strategyAction = new StrategyMoveController.StrategyAction.StrategyCustomAction();
			break;
		case DROP:
			strategyAction = new StrategyMoveController.StrategyAction.StrategyDropAction();
			consumer.accept(strategyAction);
			dropStrategyAction(strategyAction.getOperatingGroup().groupId);
			return 0;
		}
		consumer.accept(strategyAction);
		strategyAction.setStrategyMoveController(this);
		if(!strategyActionDeque.containsKey(strategyAction.getOperatingGroup().groupId)) {
			groupIdDeque.add(strategyAction.getOperatingGroup().groupId);
			strategyActionDeque.put(strategyAction.getOperatingGroup().groupId, new ArrayDeque<>());
		}
		strategyActionDeque.get(strategyAction.getOperatingGroup().groupId).add(strategyAction);
		return strategyAction.getActionId();
	}
	
	private void dropStrategyAction(int groupId) {
		if(strategyActionDeque.get(groupId).isEmpty()) {
			return;
		}
		StrategyAction.IStrategyAction strategyAction = strategyActionDeque.get(groupId).getFirst();
		strategyAction.setDrop();
	}
	
	public void repeatStrategyAction(int groupId, StrategyMoveController.StrategyAction.IStrategyAction action) {
		strategyActionDeque.get(groupId).offerFirst(action);
	}
	
	public void executeActionDequeActions(int tickNumber) {
		int nextGroupId = groupIdDeque.removeFirst();
		StrategyMoveController.StrategyAction.IStrategyAction strategyAction = strategyActionDeque.get(nextGroupId).pollFirst();
		if(strategyAction != null) {
			strategyAction.execute(tickNumber);
		}
		groupIdDeque.addLast(nextGroupId);
	}
	
	public boolean executeQueueAction(Move move) {
		Consumer<Move> moveAction = actionQueue.poll();
		if(moveAction != null) {
			moveAction.accept(move);
			return true;
		}
		return false;
	}
	
	public static class StrategyAction {
		
		public static final int CELL_COORDINATES = 1;
		public static final int EXACT_COORDINATES = 2;
		public static final int EXPAND_FACTOR = 3;
		public static final int MAX_SPEED = 4;
		public static final int CUSTOM_METHOD = 5;
		
		public interface IStrategyAction {
			
			public void addActionParameter(int parameterValue, Object actionParameter);
			
			public void setOperatingGroup(OperatingGroup operatingGroup);
			
			public OperatingGroup getOperatingGroup();
			
			public void setStrategyMoveController(StrategyMoveController strategyMoveController);
			
			public StrategyMoveController getStrategyMoveController();
			
			public void setBetweenActionDelay(int tickNumber);
			
			public void setAfterActionDelay(int tickNumber);
			
			public void waitUntilActionIds(int... actionIds);
			
			public List<Integer> getWaitUntilActionId();
			
			public int getActionId();
			
			public void setDrop();
			
			default boolean execute(int tickNumber) {
				for(int waitActionId : getWaitUntilActionId()) {
					if(!StrategyUtil.isActionExecutedMap.get(waitActionId)) {
						getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
						return false;
					}
				}
				if(getOperatingGroup().movementTickPause > tickNumber) {
					getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
					return false;
				}
				getWaitUntilActionId().clear();
				return true;
			}
		}
		
		public interface IStrategyCustomAction {
			public void execute();
		}
		
		public static class StrategyMoveAction implements IStrategyAction {
			
			public final int actionId;
			public List<Integer> waitUntilId;
			public boolean blockedByOtherAction;
			private StrategyMoveController moveController;
			private int betweenDelay;
			private int afterDelay;
			private OperatingGroup operatingGroup;
			private StrategyGrid.StrategyCell targetStrategyCell;
			private StrategyGrid.StrategyCell nextCell;
			private double maxSpeed;
			private boolean isDropped;
			private int movingTotal = 0;
			
			public StrategyMoveAction() {
				actionId = StrategyUtil.generateNextActionId();
				waitUntilId = new ArrayList<>();
				maxSpeed = 0;
				isDropped = false;
				blockedByOtherAction = false;
			}
			
			@Override
			public OperatingGroup getOperatingGroup() {
				return operatingGroup;
			}
			
			@Override
			public void setOperatingGroup(OperatingGroup operatingGroup) {
				this.operatingGroup = operatingGroup;
			}
			
			@Override
			public void setStrategyMoveController(StrategyMoveController moveController) {
				this.moveController = moveController;
			}
			
			@Override
			public StrategyMoveController getStrategyMoveController() {
				return moveController;
			}
			
			@Override
			public boolean execute(int tickNumber) {
				if(!IStrategyAction.super.execute(tickNumber)) {
					return false;
				}
				switch(operatingGroup.moveStatus) {
				case STAY:
					onOperatingGroupStay(tickNumber);
					return true;
				case MOVING:
					onOperatingGroupMoving(tickNumber);
					return true;
				default:
					getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
					return true;
				}
			}
			
			private void onOperatingGroupStay(int tickNumber) {
				
				if(operatingGroup.isGroupInCell(targetStrategyCell) || isDropped) {
					operatingGroup.movementTickPause = tickNumber + afterDelay;
					StrategyUtil.isActionExecutedMap.put(getActionId(), true);
					return;
				}
				
				nextCell = StrategyUtil.StrategyPathfind.getNextFreeStrategyCell(operatingGroup, targetStrategyCell.getCellCoordinates());
				
				if(nextCell == null) {
					if(!operatingGroup.isPositioned) {
						nextCell = operatingGroup.getCurrentCell();
						operatingGroup.isPositioned = true;
					} else {
						getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
						return;
					}
				}
				
				nextCell.reserveCellByType(operatingGroup.cellVehicleType);
				
				Vector2d realMoveStep = getRealMove(StrategyUtil.getCenterCoordinatesByIdList(operatingGroup.vehicleList), nextCell.getCellCenterCoordinates());
				
				moveController.addActionToQueue((Move move) -> {
					move.setAction(ActionType.CLEAR_AND_SELECT);
					move.setGroup(operatingGroup.groupId);
				});
				
				moveController.addActionToQueue((Move move) -> {
					move.setAction(ActionType.MOVE);
					move.setMaxSpeed(maxSpeed == 0 ? operatingGroup.getGroupMaxSpeed() : maxSpeed);
					move.setX(realMoveStep.x);
					move.setY(realMoveStep.y);
				});
				
				operatingGroup.moveStatus = OperatingGroup.MoveStatus.MOVING;
				
				getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
				
				movingTotal = 0;
			}
			
			private void onOperatingGroupMoving(int tickNumber) {
				if(operatingGroup.isGroupInCell(nextCell)) {
					nextCell.removeReserveFromCell(operatingGroup.cellVehicleType);
					operatingGroup.setOperatingGroupInCell(nextCell.getCellCoordinates());
					operatingGroup.movementTickPause = tickNumber + betweenDelay;
					operatingGroup.moveStatus = OperatingGroup.MoveStatus.STAY;
				}
				if((movingTotal++) > 100) {
					moveController.addStrategyActionToDeque(StrategyMoveController.StrategyAction.StrategyActionType.EXPAND, (strategyAction) -> {
						strategyAction.setOperatingGroup(operatingGroup);
						strategyAction.addActionParameter(StrategyMoveController.StrategyAction.EXPAND_FACTOR, 0.1);
						strategyAction.setBetweenActionDelay(120);
					});
					operatingGroup.moveStatus = OperatingGroup.MoveStatus.STAY;
					movingTotal = 0;
					return;
				}
				getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
			}
			
			@Override
			public void addActionParameter(int parameterValue, Object actionParameter) {
				switch(parameterValue) {
				case CELL_COORDINATES:
					this.targetStrategyCell = StrategyStorage.getGridByGridType(operatingGroup.gridType).get((Vector2i) actionParameter);
					break;
				case MAX_SPEED:
					this.maxSpeed = (double) actionParameter;
					break;
				}
			}
			
			@Override
			public void setBetweenActionDelay(int delay) {
				this.betweenDelay = delay;
			}
			
			@Override
			public void setAfterActionDelay(int delay) {
				this.afterDelay = delay;
			}
			
			@Override
			public int getActionId() {
				return actionId;
			}
			
			@Override
			public void waitUntilActionIds(int... actionIds) {
				for(int actionId : actionIds) {
					waitUntilId.add(actionId);
				}
			}
			
			@Override
			public List<Integer> getWaitUntilActionId() {
				return waitUntilId;
			}
			
			@Override
			public void setDrop() {
				this.isDropped = true;
			}
		}
		
		public static class StrategyShiftAction implements IStrategyAction {
			
			public final int actionId;
			
			public List<Integer> waitUntilId;
			
			private StrategyMoveController moveController;
			
			private OperatingGroup operatingGroup;
			
			private Vector2d shift;
			
			private int shiftEndTick;
			
			private int afterDelay;
			
			private double maxSpeed;
			
			public StrategyShiftAction() {
				actionId = StrategyUtil.generateNextActionId();
				waitUntilId = new ArrayList<>();
				maxSpeed = 0;
			}
			
			@Override
			public boolean execute(int tickNumber) {
				if(!IStrategyAction.super.execute(tickNumber)) {
					return false;
				}
				switch(operatingGroup.moveStatus) {
				case STAY:
					onOperatingGroupStay(tickNumber);
					return true;
				case MOVING:
					onOperatingGroupMoving(tickNumber);
					return true;
				default:
					getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
					return true;
				}
			}
			
			private void onOperatingGroupStay(int tickNumber) {
				
				operatingGroup.moveStatus = OperatingGroup.MoveStatus.MOVING;
				
				getOperatingGroup().isPositioned = false;
				
				moveController.addActionToQueue((Move move) -> {
					move.setAction(ActionType.CLEAR_AND_SELECT);
					move.setGroup(operatingGroup.groupId);
				});
				moveController.addActionToQueue((Move move) -> {
					move.setAction(ActionType.MOVE);
					move.setMaxSpeed(maxSpeed == 0 ? operatingGroup.getGroupMaxSpeed() : maxSpeed);
					move.setX(shift.x);
					move.setY(shift.y);
				});
				
				shiftEndTick = (int) (StrictMath.round(shift.length() / operatingGroup.getGroupMaxSpeed()) + afterDelay) + tickNumber;
				
				getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
			}
			
			private void onOperatingGroupMoving(int tickNumber) {
				if(tickNumber > shiftEndTick) {
					operatingGroup.moveStatus = OperatingGroup.MoveStatus.STAY;
					StrategyUtil.isActionExecutedMap.put(getActionId(), true);
					return;
				}
				getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
			}
			
			@Override
			public void addActionParameter(int parameterValue, Object actionParameter) {
				switch(parameterValue) {
				case EXACT_COORDINATES:
					this.shift = (Vector2d) actionParameter;
					break;
				case MAX_SPEED:
					this.maxSpeed = (double) actionParameter;
					break;
				}
			}
			
			@Override
			public void setOperatingGroup(OperatingGroup operatingGroup) {
				this.operatingGroup = operatingGroup;
			}
			
			@Override
			public OperatingGroup getOperatingGroup() {
				return operatingGroup;
			}
			
			@Override
			public void setStrategyMoveController(StrategyMoveController strategyMoveController) {
				this.moveController = strategyMoveController;
			}
			
			@Override
			public StrategyMoveController getStrategyMoveController() {
				return moveController;
			}
			
			@Override
			public void setBetweenActionDelay(int delay) {
				//No use
			}
			
			@Override
			public void setAfterActionDelay(int delay) {
				this.afterDelay = delay;
			}
			
			@Override
			public int getActionId() {
				return actionId;
			}
			
			@Override
			public void waitUntilActionIds(int... actionIds) {
				for(int actionId : actionIds) {
					waitUntilId.add(actionId);
				}
			}
			
			@Override
			public List<Integer> getWaitUntilActionId() {
				return waitUntilId;
			}
			
			@Override
			public void setDrop() {
				// TODO Auto-generated method stub
			}
		}
		
		public static class StrategyExpandAction implements IStrategyAction {
			
			public final int actionId;
			
			public List<Integer> waitUntilId;
			
			private StrategyMoveController moveController;
			
			private OperatingGroup operatingGroup;
			
			private StrategyGrid.StrategyCell targetStrategyCell;
			
			private Vector2d exactCoordinates;
			
			private double expandFactor;
			
			private int expandEndTick;
			
			private double maxSpeed;
			
			private int afterDelay;
			
			public StrategyExpandAction() {
				actionId = StrategyUtil.generateNextActionId();
				waitUntilId = new ArrayList<>();
			}
			
			@Override
			public boolean execute(int tickNumber) {
				if(!IStrategyAction.super.execute(tickNumber)) {
					return false;
				}
				switch(operatingGroup.moveStatus) {
				case STAY:
					onOperatingGroupStay(tickNumber);
					return true;
				case EXPANDING:
					onOperatingGroupExpand(tickNumber);
					return true;
				default:
					getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
					return true;
				}
			}
			
			private void onOperatingGroupStay(int tickNumber) {
				
				operatingGroup.moveStatus = OperatingGroup.MoveStatus.EXPANDING;
				
				if(targetStrategyCell == null) {
					if(operatingGroup.isPositioned) {
						targetStrategyCell = operatingGroup.getCurrentCell();
					} else {
						targetStrategyCell = StrategyStorage.getGridByGridType(operatingGroup.gridType).get(StrategyUtil.getClosestCellByCoordinates(operatingGroup));
					}
				}
				
				Vector2d expandCenter = (exactCoordinates != null) ? exactCoordinates : StrategyStorage.getGridByGridType(operatingGroup.gridType).get(targetStrategyCell.getCellCoordinates()).getCellCenterCoordinates();
				
				double expandFactorCalculated = expandFactor / operatingGroup.currentExpandFactor;
				
				if(expandFactorCalculated < 0.1) {
					expandFactorCalculated = 0.1;
				}
				if(expandFactorCalculated > 10) {
					expandFactorCalculated = 10;
				}
				
				double expandFactorCalculatedFinal = expandFactorCalculated;
				
				moveController.addActionToQueue((Move move) -> {
					move.setAction(ActionType.CLEAR_AND_SELECT);
					move.setGroup(operatingGroup.groupId);
				});
				moveController.addActionToQueue((Move move) -> {
					move.setAction(ActionType.SCALE);
					move.setFactor(expandFactorCalculatedFinal);
					move.setMaxSpeed(maxSpeed == 0 ? operatingGroup.getGroupMaxSpeed() : maxSpeed);
					move.setX(expandCenter.x);
					move.setY(expandCenter.y);
				});
				
				getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
			}
			
			private void onOperatingGroupExpand(int tickNumber) {
				if(tickNumber > expandEndTick) {
					operatingGroup.currentExpandFactor = expandFactor;
					operatingGroup.moveStatus = OperatingGroup.MoveStatus.STAY;
					operatingGroup.movementTickPause = tickNumber + afterDelay;
					StrategyUtil.isActionExecutedMap.put(getActionId(), true);
					return;
				}
				getStrategyMoveController().repeatStrategyAction(getOperatingGroup().groupId, this);
			}
			
			@Override
			public void setOperatingGroup(OperatingGroup operatingGroup) {
				this.operatingGroup = operatingGroup;
			}
			
			@Override
			public OperatingGroup getOperatingGroup() {
				return this.operatingGroup;
			}
			
			@Override
			public void setStrategyMoveController(StrategyMoveController strategyMoveController) {
				this.moveController = strategyMoveController;
			}
			
			@Override
			public StrategyMoveController getStrategyMoveController() {
				return this.moveController;
			}
			
			@Override
			public void setBetweenActionDelay(int delay) {
				//No use
			}
			
			@Override
			public void setAfterActionDelay(int delay) {
				this.afterDelay = delay;
			}
			
			@Override
			public void addActionParameter(int parameterValue, Object actionParameter) {
				switch(parameterValue) {
				case CELL_COORDINATES:
					this.targetStrategyCell = StrategyStorage.getGridByGridType(operatingGroup.gridType).get((Vector2i) actionParameter);
					break;
				case EXPAND_FACTOR:
					this.expandFactor = (double) actionParameter;
					break;
				case EXACT_COORDINATES:
					this.exactCoordinates = (Vector2d) actionParameter;
					break;
				case MAX_SPEED:
					this.maxSpeed = (double) actionParameter;
					break;
				}
			}
			
			@Override
			public int getActionId() {
				return actionId;
			}
			
			@Override
			public void waitUntilActionIds(int... actionIds) {
				for(int actionId : actionIds) {
					waitUntilId.add(actionId);
				}
			}
			
			@Override
			public List<Integer> getWaitUntilActionId() {
				return waitUntilId;
			}
			
			@Override
			public void setDrop() {
				// TODO Auto-generated method stub
			}
		}
		
		public static class StrategyCustomAction implements IStrategyAction {
			
			public final int actionId;
			
			public List<Integer> waitUntilId;
			
			private IStrategyCustomAction customMethod;
			
			private StrategyMoveController moveController;
			
			private OperatingGroup operatingGroup;
			
			public StrategyCustomAction() {
				actionId = StrategyUtil.generateNextActionId();
				waitUntilId = new ArrayList<>();
			}
			
			@Override
			public boolean execute(int tickNumber) {
				if(!IStrategyAction.super.execute(tickNumber)) {
					return false;
				}
				customMethod.execute();
				return true;
			}
			
			@Override
			public void addActionParameter(int parameterValue, Object actionParameter) {
				if(parameterValue == CUSTOM_METHOD) {
					customMethod = (IStrategyCustomAction) actionParameter;
				}
			}
			
			@Override
			public void setOperatingGroup(OperatingGroup operatingGroup) {
				this.operatingGroup = operatingGroup;
			}
			
			@Override
			public OperatingGroup getOperatingGroup() {
				return this.operatingGroup;
			}
			
			@Override
			public void setStrategyMoveController(StrategyMoveController strategyMoveController) {
				this.moveController = strategyMoveController;
			}
			
			@Override
			public StrategyMoveController getStrategyMoveController() {
				return this.moveController;
			}
			
			@Override
			public void setBetweenActionDelay(int delay) {
				
			}
			
			@Override
			public void setAfterActionDelay(int delay) {
				
			}
			
			@Override
			public int getActionId() {
				return actionId;
			}
			
			@Override
			public void waitUntilActionIds(int... actionIds) {
				for(int actionId : actionIds) {
					waitUntilId.add(actionId);
				}
			}
			
			@Override
			public List<Integer> getWaitUntilActionId() {
				return waitUntilId;
			}
			
			@Override
			public void setDrop() {
				// TODO Auto-generated method stub
			}
		}
		
		public static class StrategyDropAction implements IStrategyAction {
			
			private StrategyMoveController moveController;
			
			private OperatingGroup operatingGroup;
			
			@Override
			public void addActionParameter(int parameterValue, Object actionParameter) {
				
			}
			
			@Override
			public void setOperatingGroup(OperatingGroup operatingGroup) {
				this.operatingGroup = operatingGroup;
			}
			
			@Override
			public OperatingGroup getOperatingGroup() {
				return this.operatingGroup;
			}
			
			@Override
			public void setStrategyMoveController(StrategyMoveController strategyMoveController) {
				this.moveController = strategyMoveController;
			}
			
			@Override
			public StrategyMoveController getStrategyMoveController() {
				return this.moveController;
			}
			
			@Override
			public void setBetweenActionDelay(int tickNumber) {
				
			}
			
			@Override
			public void setAfterActionDelay(int tickNumber) {
				
			}
			
			@Override
			public void waitUntilActionIds(int... actionIds) {
				
			}
			
			@Override
			public List<Integer> getWaitUntilActionId() {
				return null;
			}
			
			@Override
			public int getActionId() {
				return 0;
			}
			
			@Override
			public void setDrop() {
				
			}
		}
		
		public enum StrategyActionType {
			DROP, MOVE, SHIFT, EXPAND, CUSTOM;
		}
	}
}
