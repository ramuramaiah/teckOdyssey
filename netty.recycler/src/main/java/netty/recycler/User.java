package netty.recycler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.util.internal.ObjectPool;
import io.netty.util.internal.ObjectPool.Handle;
import io.netty.util.internal.ObjectPool.ObjectCreator;

@JsonRootName(value = "user")
@JsonIgnoreProperties({ "handle", "creator", "RECYCLER" })
public class User {
	private String firstName;
	private String lastName;
	private String email;
	
	private final Handle<User> handle;
	
	private static ObjectCreator<User> creator = (handle) -> new User(handle);
	private static final ObjectPool<User> RECYCLER = ObjectPool.newPool(creator);
	
	public static ObjectPool<User> getRecycler() {
		return RECYCLER;
	}

	public void recycle() {
        clear();
        handle.recycle(this);
    }
	
	private void clear() {
		firstName = null;
		lastName = null;
		email = null;
	}
	
	public String toJson() throws JsonProcessingException {
		String json = new ObjectMapper().writeValueAsString(this);
		return json;
	}
	
	public static User fromJson(String json) throws JsonMappingException, JsonProcessingException {
		User user = RECYCLER.get();
		new ObjectMapper().readerForUpdating(user).readValue(json);
		return user;
	}
	
	private User(Handle<User> handle) {
		this.handle = handle;
	}
	
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
		result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		User other = (User) obj;
		if (email == null || other.email != null)
			return false;
		else if (!email.equals(other.email))
			return false;
		
		if (firstName == null || other.firstName != null)
			return false;
		else if (!firstName.equals(other.firstName))
			return false;
		
		if (lastName == null || other.lastName != null)
			return false;
		else if (!lastName.equals(other.lastName))
			return false;
				
		return true;
	}
}
