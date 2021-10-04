package elastic.recipes.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Recipe implements Serializable {

	private static final long serialVersionUID = 1L;
	private String title;
	private String description;
	private List<String> ingredients;
	@JsonProperty("preparation_time_minutes")
	private int preparationTime;
	private Servings servings;
	@JsonProperty("inserted_at")
	private Date insertedAt;
	
	public Recipe() {
		
	}
	
	public Recipe(String title, String description, List<String> ingredients, int preparationTime, Servings servings, Date insertedAt) {
		super();
		this.setTitle(title);
		this.setDescription(description);
		this.setIngredients(ingredients);
		this.setPreparationTime(preparationTime);
		this.setServings(servings);
		this.setInsertedAt(insertedAt);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<String> getIngredients() {
		return ingredients;
	}

	public void setIngredients(List<String> ingredients) {
		this.ingredients = ingredients;
	}

	public int getPreparationTime() {
		return preparationTime;
	}

	public void setPreparationTime(int preparationTime) {
		this.preparationTime = preparationTime;
	}

	public Servings getServings() {
		return servings;
	}

	public void setServings(Servings servings) {
		this.servings = servings;
	}

	public Date getInsertedAt() {
		return insertedAt;
	}

	public void setInsertedAt(Date insertedAt) {
		this.insertedAt = insertedAt;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
	@Override
	public boolean equals(Object o) {
		return EqualsBuilder.reflectionEquals(this, o);
	}
}
