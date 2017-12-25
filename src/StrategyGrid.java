import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import model.TerrainType;
import model.WeatherType;
import model.World;

/**
 * Russian AI Cup 2017 (Code Wars contest)
 * <br>Grid class with X1 and X2 cells
 * 
 * @since 2017
 * @author Kunik
 */

public class StrategyGrid {
	
	public static double cellStepX;
	public static double cellStepY;
	
	public static StrategyCellX1 getStrategyCellX1ByCoordinates(double x, double y) {
		Vector2i cellCoordinates = new Vector2i((int) StrictMath.floor(x / cellStepX), (int) StrictMath.floor(y / cellStepY));
		Map<Vector2i, ? extends StrategyCell> gridMap = StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1);
		if(gridMap.containsKey(cellCoordinates)) {
			return (StrategyCellX1) gridMap.get(cellCoordinates);
		}
		return null;
	}
	
	public static void setupStrategyGrid(World world) {
		
		StrategyGrid grid = new StrategyGrid();
		
		TerrainType[][] terrainType = world.getTerrainByCellXY();
		
		cellStepX = world.getWidth() / terrainType.length;
		cellStepY = world.getHeight() / terrainType[0].length;
		
		grid.setupStrategyGrid1X(world);
		grid.setupStrategyGrid2X(world);
	}
	
	private void setupStrategyGrid1X(World world) {
		
		TerrainType[][] terrainType = world.getTerrainByCellXY();
		WeatherType[][] weatherType = world.getWeatherByCellXY();
		
		for(int cellX = 0; cellX < terrainType.length; cellX++) {
			for(int cellY = 0; cellY < terrainType[cellX].length; cellY++) {
				StrategyStorage.putCellByGridType(OperatingGroup.GridType.X1, new Vector2i(cellX, cellY), new StrategyCellX1(cellX, cellY, terrainType[cellX][cellY], weatherType[cellX][cellY]));
			}
		}
	}
	
	private void setupStrategyGrid2X(World world) {
		
		TerrainType[][] terrainType = world.getTerrainByCellXY();
		
		for(int cellX = 0; cellX < (terrainType.length - 1); cellX++) {
			for(int cellY = 0; cellY < (terrainType[cellX].length - 1); cellY++) {
				
				StrategyCellX2 cellX2 = new StrategyCellX2(cellX, cellY);
				
				Map<Vector2i, ? extends StrategyCell> gridX1 = StrategyStorage.getGridByGridType(OperatingGroup.GridType.X1);
				
				cellX2.cellsX1.add((StrategyCellX1) gridX1.get(new Vector2i(cellX, cellY)));
				cellX2.cellsX1.add((StrategyCellX1) gridX1.get(new Vector2i(cellX + 1, cellY)));
				cellX2.cellsX1.add((StrategyCellX1) gridX1.get(new Vector2i(cellX, cellY + 1)));
				cellX2.cellsX1.add((StrategyCellX1) gridX1.get(new Vector2i(cellX + 1, cellY + 1)));
				
				StrategyStorage.putCellByGridType(OperatingGroup.GridType.X2, new Vector2i(cellX, cellY), cellX2);
			}
		}
	}
	
	public interface StrategyCell {
		
		public Rectangle getCellRectangle();
		
		public Vector2d getCellCenterCoordinates();
		
		public Vector2i getCellCoordinates();
		
		public void reserveCellByType(StrategyGrid.CellVehicleType cellVehicleType);
		
		public boolean isCellBusy(CellVehicleType cellVehicleType, long sameGroupId);
		
		public void removeReserveFromCell(StrategyGrid.CellVehicleType cellVehicleType);
		
		public void setOperatingGroupInCell(OperatingGroup operatingGroup);
		
		public OperatingGroup getOperatingGroupInCell(StrategyGrid.CellVehicleType cellVehicleType);
		
		public void checkAndRemoveOperatingGroupInCell();
		
		public boolean isFriendsExistsOnCell(long playerId, CellVehicleType cellVehicleType);
	}
	
	public class StrategyCellX1 implements StrategyCell {
		
		public int cellX;
		public int cellY;
		
		public TerrainType terrainType;
		public WeatherType weatherType;
		
		public List<ExtendedGameObjects.VehicleExtended> groundVehiclesOnCell;
		public List<ExtendedGameObjects.VehicleExtended> flightVehiclesOnCell;
		
		public OperatingGroup groundOperatingGroup;
		public OperatingGroup flightOperatingGroup;
		
		private final boolean[] isCellReserved;
		
		private final Rectangle rectangle;
		
		public StrategyCellX1(int cellX, int cellY, TerrainType terrainType, WeatherType weatherType) {
			this.cellX = cellX;
			this.cellY = cellY;
			this.terrainType = terrainType;
			this.weatherType = weatherType;
			this.groundVehiclesOnCell = new ArrayList<>();
			this.flightVehiclesOnCell = new ArrayList<>();
			this.isCellReserved = new boolean[]{false, false};
			rectangle = new Rectangle();
			rectangle.setRect(cellX * cellStepX + 2, cellY * cellStepY + 2, cellStepX - 4, cellStepY - 4);
		}
		
		@Override
		public Vector2d getCellCenterCoordinates() {
			return new Vector2d((cellX * cellStepX) + (cellStepX / 2), (cellY * cellStepY) + (cellStepY / 2));
		}
		
		@Override
		public Vector2i getCellCoordinates() {
			return new Vector2i(cellX, cellY);
		}
		
		@Override
		public void setOperatingGroupInCell(OperatingGroup operatingGroup) {
			switch(operatingGroup.cellVehicleType) {
			case GROUND:
				groundOperatingGroup = operatingGroup;
				break;
			case FLIGHT:
				flightOperatingGroup = operatingGroup;
				break;
			case MIXED:
				groundOperatingGroup = operatingGroup;
				flightOperatingGroup = operatingGroup;
				break;
			}
		}
		
		@Override
		public OperatingGroup getOperatingGroupInCell(CellVehicleType cellVehicleType) {
			switch(cellVehicleType) {
			case GROUND:
				return this.groundOperatingGroup;
			case FLIGHT:
				return this.flightOperatingGroup;
			case MIXED:
				return this.groundOperatingGroup;
			}
			return null;
		}
		
		public void checkAndRemoveOperatingGroupInCell() {
			if(groundVehiclesOnCell.isEmpty()) {
				groundOperatingGroup = null;
			}
			if(flightVehiclesOnCell.isEmpty()) {
				flightOperatingGroup = null;
			}
		}
		
		@Override
		public void reserveCellByType(StrategyGrid.CellVehicleType cellVehicleType) {
			switch(cellVehicleType) {
			case GROUND:
				this.isCellReserved[0] = true;
				break;
			case FLIGHT:
				this.isCellReserved[1] = true;
				break;
			case MIXED:
				this.isCellReserved[0] = true;
				this.isCellReserved[1] = true;
				break;
			}
		}
		
		@Override
		public boolean isCellBusy(CellVehicleType cellVehicleType, long sameGroupId) {
			switch(cellVehicleType) {
			case GROUND:
				return (this.isCellReserved[0] || (!groundVehiclesOnCell.isEmpty() && groundVehiclesOnCell.stream().filter((ve) -> !ve.getGroups().contains(sameGroupId)).findAny().isPresent()));
			case FLIGHT:
				return (this.isCellReserved[1] || (!flightVehiclesOnCell.isEmpty() && flightVehiclesOnCell.stream().filter((ve) -> !ve.getGroups().contains(sameGroupId)).findAny().isPresent()));
			case MIXED:
				return (isCellBusy(CellVehicleType.GROUND, sameGroupId) || isCellBusy(CellVehicleType.FLIGHT, sameGroupId));
			}
			return false;
		}
		
		@Override
		public void removeReserveFromCell(CellVehicleType cellVehicleType) {
			switch(cellVehicleType) {
			case GROUND:
				this.isCellReserved[0] = false;
				break;
			case FLIGHT:
				this.isCellReserved[1] = false;
				break;
			case MIXED:
				this.isCellReserved[0] = false;
				this.isCellReserved[1] = false;
				break;
			}
		}
		
		@Override
		public Rectangle getCellRectangle() {
			return rectangle;
		}
		
		@Override
		public boolean isFriendsExistsOnCell(long playerId, CellVehicleType cellVehicleType) {
			switch(cellVehicleType) {
			case GROUND:
				return this.groundVehiclesOnCell.stream().anyMatch((vehicle) -> {
					return vehicle.vehicle.getPlayerId() == playerId;
				});
			case FLIGHT:
				return this.flightVehiclesOnCell.stream().anyMatch((vehicle) -> vehicle.vehicle.getPlayerId() == playerId);
			case MIXED:
				return isFriendsExistsOnCell(playerId, CellVehicleType.GROUND) || isFriendsExistsOnCell(playerId, CellVehicleType.FLIGHT);
			}
			return false;
		}
	}
	
	public class StrategyCellX2 implements StrategyCell {
		
		public int cellX;
		public int cellY;
		
		public List<StrategyCellX1> cellsX1;
		
		private final Rectangle rectangle;
		
		public StrategyCellX2(int cellX, int cellY) {
			this.cellsX1 = new ArrayList<>(4);
			this.cellX = cellX;
			this.cellY = cellY;
			rectangle = new Rectangle();
			rectangle.setRect(cellX * cellStepX, cellY * cellStepY, cellStepX * 2, cellStepY * 2);
		}
		
		@Override
		public Vector2d getCellCenterCoordinates() {
			return new Vector2d((cellX * cellStepX) + (cellStepX), (cellY * cellStepY) + (cellStepY));
		}
		
		@Override
		public Vector2i getCellCoordinates() {
			return new Vector2i(cellX, cellY);
		}
		
		@Override
		public void reserveCellByType(StrategyGrid.CellVehicleType cellVehicleType) {
			for(StrategyCell cellX1 : cellsX1) {
				cellX1.reserveCellByType(cellVehicleType);
			}
		}
		
		public boolean isCellBusy(CellVehicleType cellVehicleType, long sameGroupId) {
			for(StrategyCell cellX1 : cellsX1) {
				if(cellX1.isCellBusy(cellVehicleType, sameGroupId)) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public void removeReserveFromCell(StrategyGrid.CellVehicleType cellVehicleType) {
			for(StrategyCell cellX1 : cellsX1) {
				cellX1.removeReserveFromCell(cellVehicleType);
			}
		}
		
		@Override
		public Rectangle getCellRectangle() {
			return rectangle;
		}
		
		@Override
		public void setOperatingGroupInCell(OperatingGroup operatingGroup) {
			for(StrategyCell cellX1 : cellsX1) {
				cellX1.setOperatingGroupInCell(operatingGroup);
			}
		}
		
		@Override
		public OperatingGroup getOperatingGroupInCell(CellVehicleType cellVehicleType) {
			for(StrategyCellX1 cellX1 : cellsX1) {
				OperatingGroup operatingGroup = cellX1.getOperatingGroupInCell(cellVehicleType);
				if(operatingGroup != null) {
					return operatingGroup;
				}
			}
			return null;
		}
		
		@Override
		public void checkAndRemoveOperatingGroupInCell() {
			for(StrategyCell cellX1 : cellsX1) {
				cellX1.checkAndRemoveOperatingGroupInCell();
			}
		}
		
		public List<Long> getGroundVehiclesOnCell() {
			List<Long> vehicles = new ArrayList<>();
			for(StrategyCellX1 cellX1 : cellsX1) {
				cellX1.groundVehiclesOnCell.stream().mapToLong(ExtendedGameObjects.VehicleExtended::getId).forEach((l) -> vehicles.add(l));
			}
			return vehicles;
		}
		
		public List<Long> getFlightVehiclesOnCell() {
			List<Long> vehicles = new ArrayList<>();
			for(StrategyCellX1 cellX1 : cellsX1) {
				cellX1.flightVehiclesOnCell.stream().mapToLong(ExtendedGameObjects.VehicleExtended::getId).forEach((l) -> vehicles.add(l));
			}
			return vehicles;
		}
		
		//TODO: Broken, need to fix
		public boolean isFriendsExistsOnCell(long playerId, CellVehicleType cellVehicleType) {
			int existCount = 0;
			for(StrategyCell cellX1 : cellsX1) {
				if(cellX1.isFriendsExistsOnCell(playerId, cellVehicleType)) {
					existCount++;
				}
			}
			return (existCount > 2);
		}
	}
	
	public enum CellVehicleType {
		GROUND, FLIGHT, MIXED;
	}
}
