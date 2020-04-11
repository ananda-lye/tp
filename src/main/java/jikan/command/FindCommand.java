package jikan.command;

import jikan.activity.Activity;
import jikan.activity.ActivityList;
import jikan.exception.EmptyNameException;
import jikan.exception.ExtraParametersException;
import jikan.exception.InvalidCommandException;
import jikan.exception.MultipleDelimitersException;
import jikan.exception.EmptyQueryException;
import jikan.ui.Ui;

import static jikan.Jikan.lastShownList;
import java.util.ArrayList;

/**
 * Represents a command to find activities by name.
 */
public class FindCommand extends Command {
    boolean isFinalCommand;
    boolean isChained;
    private static final String FILTER = "filter";
    private static final String FIND = "find";

    /**
     * Constructor to create a new find command.
     */
    public FindCommand(String parameters) throws MultipleDelimitersException {
        super(parameters.trim());
        isFinalCommand = true;
        this.parameters = parameters.replaceAll("\\s+", " ");
        this.parameters = parameters.trim();
        if (parameters.contains(";;") || parameters.contains("; ;")) {
            throw new MultipleDelimitersException();
        }
    }

    /**
     * Constructor to create a new find command that has chaining.
     */
    public FindCommand(String parameters, boolean isFinal, boolean hasChaining) throws MultipleDelimitersException {
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
     * Shows the user all past activities that has names which match the keyword queried by the user.
     * @param activityList the activity list to search for matching activities
     */
    @Override
    public void executeCommand(ActivityList activityList) {
        String[] tokenizedParameters = parameters.split(" ;", 2);
        try {
            checkForInvalidChaining();
        } catch (MultipleDelimitersException e) {
            Ui.printDivider("Please only use one ';' between each command.");
            return;
        }

        if (tokenizedParameters.length > 1) {
            executeChainedCommand(activityList, tokenizedParameters);
        } else {
            isFinalCommand = true;
            executeSingleCommand(activityList);
        }
    }

    private void checkForInvalidChaining() throws MultipleDelimitersException {
        if (parameters.contains(";;") || parameters.contains("; ;")) {
            throw new MultipleDelimitersException();
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
     * Find activities which has names containing the keywords from the entire list.
     * @param activityList full like of activities
     */
    private void searchFullList(ActivityList activityList) {
        try {
            if (parameters.length() < 1) {
                throw new EmptyQueryException();
            } else {
                String[] keywords = parameters.split(" / ");
                lastShownList.activities.clear();
                for (String keyword : keywords) {
                    populateLastShownList(keyword, activityList.activities);
                }
                callPrintResults();
            }
        } catch (EmptyQueryException e) {
            Ui.printDivider("No keyword was given.");
        }
    }

    /**
     * Find activities which has names containing the keywords from the last shown list.
     */
    private void searchSubList() {
        try {
            String query = parameters.replace("-s ", "");
            ArrayList<Activity> prevList = new ArrayList<>();
            prevList.addAll(lastShownList.activities);
            if (query.length() < 1) {
                throw new EmptyQueryException();
            } else {
                String[] keywords = query.split(" / ");
                lastShownList.activities.clear();
                for (String keyword : keywords) {
                    populateLastShownList(keyword, prevList);
                }
            }
            callPrintResults();
        } catch (ArrayIndexOutOfBoundsException | EmptyQueryException e) {
            Ui.printDivider("No keyword was given.");
        }
    }

    private void callPrintResults() {
        if (isFinalCommand == true) {
            Ui.printResults(lastShownList);
        }
    }

    /**
     * Fills the last shown list with the results from matching names of activities to a keyword.
     * @param keyword the keyword to match against
     * @param activities the list of activities to search
     */
    private void populateLastShownList(String keyword, ArrayList<Activity> activities) {
        for (Activity i : activities) {
            if (containsIgnoreCase(i.getName(), keyword) && !lastShownList.activities.contains(i)) {
                lastShownList.activities.add(i);
            }
        }
    }

    private boolean containsIgnoreCase(String str, String subString) {
        return str.toLowerCase().contains(subString.toLowerCase());
    }

}
