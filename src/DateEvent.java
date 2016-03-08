import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class DateEvent implements Serializable {
    protected static final long serialVersionUID = 1112122200L;

	Date startDate;
	Date endDate;
	String creator;
	String title;
	String description;
	Integer eventID;
	Date updatedTimestamp;
	
	//0 is invited, 1 is not going, 2 is maybe, 3 is going
	Map<String, Integer> members = new HashMap<String, Integer>();
	
	public DateEvent(Date start, Date end, String descript, String heading, String starter) {
		startDate = start;
		endDate = end;
		description = descript;
		title = heading;
		creator = starter;
	}
	
	public Integer getID() {
		return eventID;
	}
	
	public void setID(Integer id) {
		eventID = id;
	}
	
	public Date getStart() {
		return startDate;
	}
	
	public void setStart(Date date) {
		startDate = date;
	}
	
	public Date getTimestamp() {
		return updatedTimestamp;
	}
	
	public void setTimestamp(Date date) {
		updatedTimestamp = date;
	}
	
	public Date getEnd() {
		return endDate;
	}
	
	public void setEnd(Date date) {
		endDate = date;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String descript) {
		description = descript;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String heading) {
		title = heading;
	}
	
	public String getCreator() {
		return creator;
	}
	
	public void setCreator(String starter) {
		creator = starter;
	}
	
	public Map<String, Integer> getMembers() {
		return members;
	}
	
	public Integer setMemberFlag(String name, Integer flag) {
		return members.put(name, flag);
	}
	
	public Integer removeMember(String name) {
		return members.remove(name);
	}
}
