package io.nots.intellij.editor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class NotsIcon {
    @JsonProperty("url")
    public String url;
    @JsonProperty("title")
    public String title;
    @JsonProperty("line_number")
    public int lineNumber;
    @JsonProperty("id")
    public String Id;
    @JsonProperty("icon_url")
    public String iconURL;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotsIcon notsIcon = (NotsIcon) o;
        return lineNumber == notsIcon.lineNumber &&
                Objects.equals(url, notsIcon.url) &&
                Objects.equals(title, notsIcon.title) &&
                Objects.equals(Id, notsIcon.Id) &&
                Objects.equals(iconURL, notsIcon.iconURL);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, title, lineNumber, Id, iconURL);
    }
}
