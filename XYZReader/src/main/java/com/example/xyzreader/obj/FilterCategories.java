package com.example.xyzreader.obj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class FilterCategories {

    private static final String FILTER_BY = "FILTER_BY";
    private static final String FILTER_ON = "FILTER_ON";

    private Map<String, ArrayList<Option>> optionMap;
    private Option activeOption;

    public FilterCategories(String[] filterByOptions, String[] filterOnOption) {
        ArrayList<Option> optionsBy = new ArrayList<>();
        for(String option : filterOnOption) {
            optionsBy.add(new Option(option, filterByOptions));
        }

        optionMap.put(FILTER_BY, optionsBy);

        ArrayList<Option> optionsOn = new ArrayList<>();
        for(String option : filterOnOption) {
            optionsOn.add(new Option(option, filterByOptions));
        }
        optionMap.put(FILTER_ON, optionsOn);
    }

    public class Option {

        private String optionName;
        private ArrayList<String> subOptions;
        private int activeSubOptionIndex = 0;

        public Option(String optionName, String[] subOptions) {
            this.optionName = optionName;
            this.subOptions = new ArrayList<>(Arrays.asList(subOptions));

            if(this.subOptions.isEmpty())
                activeSubOptionIndex = -1;
        }
    }
}
