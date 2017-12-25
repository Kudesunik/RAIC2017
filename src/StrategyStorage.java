import java.util.HashMap;
import java.util.Map;

public final class StrategyStorage {
	
	public static long playerId;
	public static int maxUnitGroup;
	
	public static boolean canDeployNuclear = true;
	
	public static final Map<Long, ExtendedGameObjects.VehicleExtended> vehicleExtendedById = new HashMap<>();
	public static final Map<Long, ExtendedGameObjects.FacilityExtended> facilityExtendedById = new HashMap<>();
	public static final Map<String, OperatingGroup> operatingGroups = new HashMap<>();
	private static final Map<Vector2i, StrategyGrid.StrategyCellX1> strategyGridX1 = new HashMap<>();
	private static final Map<Vector2i, StrategyGrid.StrategyCellX2> strategyGridX2 = new HashMap<>();
	
	public static Map<Vector2i, ? extends StrategyGrid.StrategyCell> getGridByGridType(OperatingGroup.GridType gridType) {
		switch(gridType) {
		case X1:
			return strategyGridX1;
		case X2:
			return strategyGridX2;
		}
		return null;
	}
	
	public static <T extends StrategyGrid.StrategyCell> void putCellByGridType(OperatingGroup.GridType gridType, Vector2i cellPosition, T cell) {
		switch(gridType) {
		case X1:
			strategyGridX1.put(cellPosition, (StrategyGrid.StrategyCellX1) cell);
			break;
		case X2:
			strategyGridX2.put(cellPosition, (StrategyGrid.StrategyCellX2) cell);
			break;
		}
	}
}
