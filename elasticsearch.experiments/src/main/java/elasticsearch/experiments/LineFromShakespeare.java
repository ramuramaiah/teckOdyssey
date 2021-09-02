package elasticsearch.experiments;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model class, representing Line from Shakespeare
 *
 */
public class LineFromShakespeare implements Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	private String type;
	@JsonProperty("line_id")
	private Integer lineId;
	@JsonProperty("play_name")
	private String playName;
	@JsonProperty("speech_number")
	private Integer speechNumber;
	@JsonProperty("line_number")
	private String lineNumber;
	private String speaker;
	@JsonProperty("text_entry")
	private String textEntry;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Integer getLineId() {
		return lineId;
	}
	public void setLineId(Integer lineId) {
		this.lineId = lineId;
	}
	public String getPlayName() {
		return playName;
	}
	public void setPlayName(String playName) {
		this.playName = playName;
	}
	public Integer getSpeechNumber() {
		return speechNumber;
	}
	public void setSpeechNumber(Integer speechNumber) {
		this.speechNumber = speechNumber;
	}
	public String getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(String lineNumber) {
		this.lineNumber = lineNumber;
	}
	public String getSpeaker() {
		return speaker;
	}
	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}
	public String getTextEntry() {
		return textEntry;
	}
	public void setTextEntry(String textEntry) {
		this.textEntry = textEntry;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
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
