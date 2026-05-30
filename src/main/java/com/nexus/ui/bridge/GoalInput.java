package com.nexus.ui.bridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoalInput {
    public Long       id;
    public String     title;
    public String     description;
    public Long       categoryId;
    public String     targetDate;
    public List<Long> categoryIds;
}
