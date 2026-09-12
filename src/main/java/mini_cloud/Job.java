package mini_cloud;

import java.util.UUID;

public class Job{

	private UUID id;
	private String type;
	private String status;
	private String result;
	private int attempts; // for max retries
	private long createdAt; // for idempotency
	private long startedAt;
	private int priority; // for FIFO. 3 = high priority and 1 = low priority

	// For actual new jobs
	public Job(String type, int priority) { 
		this.id = UUID.randomUUID(); 
		this.type = type; 
		this.status = "QUEUED";
		this.attempts = 0; 
		this.createdAt = System.currentTimeMillis();
		this.priority = priority;
	}

	// For reconstructing jobs from Redis
	public Job(UUID id, String type, String status, String result, int attempts, long createdAt, long startedAt, int priority){
		this.id = id;
		this.type = type;
		this.status = status;
		this.result = result;
		this.attempts = attempts;
		this.createdAt = createdAt;
		this.startedAt = startedAt;
		this.priority = priority;
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

	public int getAttempts(){
		return attempts;
	}

	public void Attempted(){
		attempts++;
	}
	
	public long getCreatedAt() {
    	return createdAt;
	}

	public long getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(long startedAt) {
		this.startedAt = startedAt;
	}

	public int getPriority() {
	    return priority;
	}
}