import java.util.ArrayList;
import java.util.List;

import model.Facility;
import model.Vehicle;

/**
 * CodeWars contest 2017
 * 
 * <p>Class for all extended game objects
 * 
 * @since 2017
 * @author Kunik
 */

public final class ExtendedGameObjects {
	
	public static class VehicleExtended {
		
		public final long id;
		
		private double realX;
		private double realY;

		private int realDurability;
		
		private List<Long> groupIds;
		
		public final Vehicle vehicle;
		
		public VehicleExtended(long id, Vehicle vehicle) {
			this.id = id;
			this.vehicle = vehicle;
			this.realX = vehicle.getX();
			this.realY = vehicle.getY();
			this.realDurability = vehicle.getDurability();
			this.groupIds = new ArrayList<>();
		}
		
		public double getX() {
			return realX;
		}
		
		public double getY() {
			return realY;
		}
		
		public long getId() {
			return id;
		}
		
		public int getDurability() {
			return realDurability;
		}
		
		public void updatePosition(double x, double y) {
			realX = x;
			realY = y;
		}
		
		public void updateDurability(int durability) {
			realDurability = durability;
		}
		
		public void assignGroup(long groupId) {
			this.groupIds.add(groupId);
		}
		
		public List<Long> getGroups() {
			return groupIds;
		}
	}
	
	public enum VehicleSide {
		ANY, FRIEND, ENEMY;
	}
	
	public static class FacilityExtended {
		
		public final long id;
		public StrategyGrid.StrategyCellX2 cell;
		public int startProductionTick;
		public int stopProductionTick;
		private Facility facility;
		
		public FacilityExtended(long id, Facility facility) {
			this.id = id;
			this.facility = facility;
			this.startProductionTick = 0;
			this.stopProductionTick = 0;
		}
		
		public void setFacility(Facility facility) {
			this.facility = facility;
		}
		
		public Facility getFacility() {
			return facility;
		}
	}
}
