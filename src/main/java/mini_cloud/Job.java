package mini_cloud;

import java.util.UUID;

public class Job{

	private UUID id;
	private String type;
	private String status;
	private String result;
	
	public Job(String type){
		this.id = UUID.randomUUID();
		this.type = type;
		this.status = "QUEUED";
	}

	public UUID getId(){
		return id;
	}

	public String getType(){
		return type;
	}
	
	public String getStatus(){
		return status;
	}

	public String getResult(){
		return result;
	}

	public void setStatus(String status){
		this.status = status;
	}

	public void setResult(String result){
		this.result = result;
	}
	
}