package jikan.command;

import jikan.activity.Activity;
import jikan.activity.ActivityList;
import jikan.exception.EmptyNameException;
import jikan.exception.ExtraParametersException;
import jikan.exception.InvalidCommandException;
import jikan.exception.MultipleDelimitersException;
import jikan.exception.EmptyQueryException;
import jikan.ui.Ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jikan.Jikan.lastShownList;

/**
 * Represents a command to filter activities by specified tags.
 */
public class FilterCommand extends Command {
    boolean isFinalCommand;
    boolean isChained;
    private static final String FILTER = "filter";
    private static final String FIND = "find";

    /**
     * Constructor to create a new filter command.
     */
    public FilterCommand(String parameters) throws MultipleDelimitersException {
        super(parameters);
        isFinalCommand = true;
        this.parameters = parameters.replaceAll("\\s+", " ");
        this.parameters = parameters.trim();
        if (parameters.contains(";;") || parameters.contains("; ;")) {
            throw new MultipleDelimitersException();
        }
    }

    /**
     * Constructor to create a new filter command with chaining.
     */
    public FilterCommand(String parameters, boolean isFinal, boolean hasChaining) throws MultipleDelimitersException {
        super(parameters.trim());
        isFinalCommand = isFinal;
        isChained = hasChaining;
        this.parameters = parameters.replaceAll("\\s+", " ");
        this.parameters = parameters.trim();
        if (parameters.contains(";;") || parameters.contains("; ;")) {
            throw new MultipleDelimitersException();
        }
    }

    /**
     * Shows the user all past activities that has tags which match the one or more keywords queried by the user.
     * @param activityList the activity list to search for matching activities
     */
    @Override
    public void executeCommand(ActivityList activityList) {
        String[] tokenizedParameters = parameters.split(";", 2);
        if (tokenizedParameters.length > 1) {
            executeChainedCommand(activityList, tokenizedParameters);
        } else {
            isFinalCommand = true;
            executeSingleCommand(activityList);
        }
    }

    private void executeChainedCommand(ActivityList activityList, String[] tokenizedParameters) {
        if (tokenizedParameters[1].length() > 0) {
            isFinalCommand = false;
            parameters = tokenizedParameters[0];
            executeSingleCommand(activityList);
            String nextCommand = tokenizedParameters[1].trim();
            try {
                callNextCommand(nextCommand, activityList);
            } catch (InvalidCommandException e) {
                Ui.printDivider("Please chain find or filter commands only");
            }
        } else {
            isFinalCommand = true;
            parameters = tokenizedParameters[0];
            searchSubList();
        }
    }

    private void executeSingleCommand(ActivityList activityList) {
        if (parameters.contains("-s") || isChained == true) {
            searchSubList();
        } else {
            searchFullList(activityList);
        }
    }

    private void callNextCommand(String userInput, ActivityList activityList) throws InvalidCommandException {
        String[] tokenizedInputs = userInput.split(" ", 2);
        String instruction = tokenizedInputs[0];
        Command command = null;
        switch (instruction) {
        case FIND:
            try {
                command = new FindCommand(tokenizedInputs[1], false, true);
            } catch (ArrayIndexOutOfBoundsException e) {
                Ui.printDivider("No keyword was given.");
            } catch (MultipleDelimitersException e) {
                Ui.printDivider("Please only use one ';' between each command.");
            }
            break;
        case FILTER:
            try {
                command = new FilterCommand(tokenizedInputs[1], false, true);
            } catch (ArrayIndexOutOfBoundsException e) {
                Ui.printDivider("No keyword was given.");
            } catch (MultipleDelimitersException e) {
                Ui.printDivider("Please only use one ';' between each command.");
            }
            break;
        default:
            throw new InvalidCommandException();
        }

        try {
            command.executeCommand(activityList);
        } catch (EmptyNameException | ExtraParametersException e) {
            Ui.printDivider("Error parsing command. Please try again.");
        }
    }

    /**
     * Filters activities by tags from the entire list of activities.
     * @param activityList the full list of activities
     */
    private void searchFullList(ActivityList activityList) {
        try {
            String query = parameters;
            if (query.length() < 1) {
                throw new EmptyQueryException();
            } else {
                lastShownList.activities.clear();
                String[] keywords = query.split(" ");
                keywords = removeBlanks(keywords);
                //keywords = keywords.filter(boolean);
                if (keywords.length < 1) {
                    throw new EmptyQueryException();
                }
                for (String keyword : keywords) {
                    populateLastShownList(activityList, lastShownList, keyword);
                }
                callPrintResults();
            }
        } catch (EmptyQueryException e) {
            Ui.printDivider("No keyword was given.");
        }
    }

    /**
     * Filter activities by tags based on the last shown list.
     */
    private void searchSubList() {
        try {
            String query = parameters.replace("-s ", "");
            ActivityList prevList = new ActivityList();
            prevList.activities.addAll(lastShownList.activities);
            if (query.length() < 1) {
                throw new EmptyQueryException();
            } else {
                lastShownList.activities.clear();
                String[] keywords = query.split(" ");
                if (keywords.length < 1) {
                    throw new EmptyQueryException();
                }
                for (String keyword : keywords) {
                    populateLastShownList(prevList, lastShownList, keyword);
                }
                callPrintResults();
            }
        } catch (ArrayIndexOutOfBoundsException | EmptyQueryException e) {
            Ui.printDivider("No keyword was given.");
        }
    }

    private void callPrintResults() {
        if (isFinalCommand == true) {
            Ui.printResults(lastShownList);
        }
    }

    private void populateLastShownList(ActivityList targetList, ActivityList lastShownList, String keyword) {
        for (Activity i : targetList.activities) {
            // if (!lastShownList.activities.contains(i) && i.getTags().contains(keyword)) {
            if (!lastShownList.activities.contains(i) && containsIgnoreCase(i.getTagsAsString(), keyword)) {
                lastShownList.activities.add(i);
            }
        }
    }

    private boolean containsIgnoreCase(String str, String subString) {
        return str.toLowerCase().contains(subString.toLowerCase());
    }

    private String[] removeBlanks(String [] strings) {
        List<String> list = new ArrayList<>(Arrays.asList(strings));
        list.removeIf(String::isBlank);
        strings = list.toArray(new String[0]);
        return strings;
    }
}
