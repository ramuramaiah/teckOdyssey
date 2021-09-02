package elasticsearch.experiments.model;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Manufacturer implements Serializable {

	private static final long serialVersionUID = 1L;
	@JsonProperty("manufacturer_name")
	private String manufacturerName;
	@JsonProperty("manufacturer_address")
	private String manufacturerAddress;
	
	public Manufacturer() {
		
	}
	public Manufacturer(String manufacturerName, String manufacturerAddress) {
		this.manufacturerName = manufacturerName;
		this.manufacturerAddress = manufacturerAddress;
	}
	public String getManufacturerName() {
		return manufacturerName;
	}
	public void setManufacturerName(String manufacturerName) {
		this.manufacturerName = manufacturerName;
	}
	public String getManufacturerAddress() {
		return manufacturerAddress;
	}
	public void setManufacturerAddress(String manufacturerAddress) {
		this.manufacturerAddress = manufacturerAddress;
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
