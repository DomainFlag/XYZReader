package com.example.xyzreader;

public class Constants {
    public static final String FILTER_PREFERENCES = "filter_preferences";

    public enum Filter {
        FILTER_PREFERENCE_OPTION_BY("filter_preference_option_by"),
        FILTER_PREFERENCE_OPTION_ON("filter_preference_option_on"),
        FILTER_PREFERENCE_SUB_OPTION("filter_preference_sub_option");

        Filter(String optionType) {}
    }
}
