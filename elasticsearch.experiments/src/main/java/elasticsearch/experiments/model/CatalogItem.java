package elasticsearch.experiments.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CatalogItem implements Serializable {

	private static final long serialVersionUID = 1L;
	private Integer id;
	private Category category;
	private String description;
	private Manufacturer manufacturer;
	@JsonProperty("sales_rank")
	private Double salesRank;
	private Double price;
	
	public CatalogItem() {
		
	}
	public CatalogItem(Integer id, Category category, String description, Manufacturer manufacturer, Double salesRank,
			Double price) {
		super();
		this.id = id;
		this.category = category;
		this.description = description;
		this.manufacturer = manufacturer;
		this.salesRank = salesRank;
		this.price = price;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Manufacturer getManufacturer() {
		return manufacturer;
	}

	public void setManufacturer(Manufacturer manufacturer) {
		this.manufacturer = manufacturer;
	}

	public Double getSalesRank() {
		return salesRank;
	}

	public void setSalesRank(Double salesRank) {
		this.salesRank = salesRank;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
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
