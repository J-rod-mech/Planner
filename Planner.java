import java.util.Scanner;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Planner {
    //constants
    final static int HALF_HOURS = 12;
    final static int HALF_DIV = 2;
    final static int HUNDRED_DIV = 100;
    final static int FIFTEEN_DIV = 15;
    final static int MIN_HOUR = 0;
    final static int MAX_HOUR = 96;
    final static int MAX_HOUR_TIME = 2400;
    final static int QUAD = 4;
    final static int HOUR_DIV = 60;
    final static int TENS_DIGIT = 10;
    final static int HOUR_OFFSET = 3;
    final static String COMMAND_ERROR = "Invalid command.";
    final static String TIME_ERROR = "Invalid time slot.";
    final static String DATE_ERROR = "Invalid date.";
    
    // custom parameters
    static String timeZone = "UTC-8"; // change depending on location
    static String directory; // assign a folder to store your schedules

    @SuppressWarnings("unchecked")
    static ArrayList<String>[] schedule = new ArrayList[MAX_HOUR + 1];
    static DateTimeFormatter myFormat = DateTimeFormatter.ofPattern("MM-dd-uuuu").withResolverStyle(ResolverStyle.STRICT);
    static String zonedDate = ZonedDateTime.now(ZoneId.of(timeZone)).format(myFormat);



    public static boolean addTask(String task, int start, int end) {
        if (start < MIN_HOUR || start > MAX_HOUR_TIME || start % 100 % 15 != 0
                || start % 100 / 60 != 0 || end < MIN_HOUR || end > MAX_HOUR_TIME
                || end % 100 % 15 != 0 || end % 100 / 60 != 0) {
            return false;
        }
        start = start / HUNDRED_DIV * QUAD + start % HUNDRED_DIV / FIFTEEN_DIV;
        end = end / HUNDRED_DIV * QUAD + end % HUNDRED_DIV / FIFTEEN_DIV;
        for (int i = start; i < end; i++) {
            if (schedule[i] == null)
                schedule[i] = new ArrayList<String>();
            schedule[i].add(task);
        }
        writeFile();
        return true;
    }

    public static boolean removeTask(String task) {
        boolean taskFound = false;
        for (int i = MIN_HOUR; i <= MAX_HOUR; i++) {
            if (schedule[i] != null) {
                for (int j = 0; j < schedule[i].size(); j++) {
                    if (schedule[i].get(j).equals(task)) {
                        schedule[i].remove(j);
                        j--;
                        taskFound = true;
                    }
                }
                if (schedule[i].size() == 0)
                    schedule[i] = null;
            }
        }
        
        if (taskFound) {
            writeFile();
        }
        return taskFound;
    }

    public static boolean removeTask(int start, int end) {
        if (start < MIN_HOUR || start > MAX_HOUR_TIME || start % 100 % 15 != 0
                || start % 100 / 60 != 0 || end < MIN_HOUR || end > MAX_HOUR_TIME
                || end % 100 % 15 != 0 || end % 100 / 60 != 0) {
            return false;
        }
        start = start / HUNDRED_DIV * QUAD + start % HUNDRED_DIV / FIFTEEN_DIV;
        end = end / HUNDRED_DIV * QUAD + end % HUNDRED_DIV / FIFTEEN_DIV;
        for (int i = start; i < end; i++) {
            schedule[i] = null;
        }
        writeFile();
        return true;
    }

    public static boolean shiftTask(String task, int shift) {
        shift = shift / HUNDRED_DIV * QUAD + shift % HUNDRED_DIV / FIFTEEN_DIV;
        String[] taskSched = new String[MAX_HOUR * 3];
        for (int i = MIN_HOUR; i <= MAX_HOUR; i++) {
            if (schedule[i] != null && schedule[i].contains(task)) {
                taskSched[i + shift + MAX_HOUR] = task;
            }
        }

        if (!removeTask(task)) {
            return false;
        }
        
        for (int i = MIN_HOUR; i < MAX_HOUR; i++) {
            if (taskSched[i + MAX_HOUR] != null) {
                if (schedule[i] == null)
                    schedule[i] = new ArrayList<String>();
                schedule[i].add(taskSched[i + MAX_HOUR]);
            }
        }

        writeFile();

        boolean goPast = false;
        for (int i = MIN_HOUR; i < MAX_HOUR; i++) {
            if (taskSched[i] != null) {
                if (!goPast) {
                    goPast = true;
                    zonedDate = LocalDate.parse(zonedDate, myFormat).plusDays(-1).format(myFormat);
                    readFile();
                }

                if (schedule[i] == null)
                    schedule[i] = new ArrayList<String>();
                schedule[i].add(taskSched[i]);
            }
        }
        writeFile();

        boolean goFor = false;
        for (int i = MIN_HOUR; i < MAX_HOUR; i++) {
            if (taskSched[i + MAX_HOUR * 2] != null) {
                if (!goFor) {
                    goFor = true;
                    zonedDate = LocalDate.parse(zonedDate, myFormat).plusDays(1).format(myFormat);
                    if (goPast) {
                        LocalDate.parse(zonedDate, myFormat).plusDays(1).format(myFormat);
                    }
                    readFile();
                }

                if (schedule[i] == null)
                    schedule[i] = new ArrayList<String>();
                schedule[i].add(taskSched[i + MAX_HOUR * 2]);
            }
        }
        writeFile();

        if (goFor) {
            zonedDate = LocalDate.parse(zonedDate, myFormat).plusDays(-1).format(myFormat);
            readFile();
        }
        else if (goPast) {
            zonedDate = LocalDate.parse(zonedDate, myFormat).plusDays(1).format(myFormat);
            readFile();
        }

        return true;
    }

    public static void printSchedule() {
        boolean isEmpty = true;

        System.out.println(zonedDate + ":");
        for (int i = MIN_HOUR; i <= MAX_HOUR; i++) {
            int j = MAX_HOUR - i;
            if ((schedule[i] != null) && (i == MIN_HOUR || !(schedule[i].equals
                    (schedule[i - 1]))) || i > MIN_HOUR && schedule[i - 1]
                    != null && !schedule[i - 1].equals(schedule[i])) {
                if ((i + 44) % 48 / 36 == 0)
                    System.out.print(" ");
                System.out.print((HALF_HOURS - (HOUR_OFFSET + j) / QUAD
                        % HALF_HOURS) + ":" + (i * FIFTEEN_DIV % HOUR_DIV));
                if (i % QUAD == 0)
                    System.out.print("0");
                if (i / HALF_HOURS / QUAD % 2 == 0)
                    System.out.print(" A");
                else
                    System.out.print(" P");
                System.out.println("M");

                if (schedule[i] != null) {
                    isEmpty = false;
                    System.out.print("  |  ");
                    for (int k = 0; k < schedule[i].size(); k++) {
                        if (k > 0)
                            System.out.print(", ");
                        System.out.print(schedule[i].get(k));
                    }
                }
                System.out.println();
            }
        }
        if (isEmpty)
            System.out.println();
    }

    public static void readFile() {
        File myObj = new File(directory + "\\scheduledata_" + zonedDate + ".txt");
        try {
            for (int i = MIN_HOUR; i <= MAX_HOUR; i++)
                schedule[i] = null;
            Scanner fileSC = new Scanner(myObj).useDelimiter("\n");
            int line = 0;
            while (fileSC.hasNext()) {
                Scanner lineSC = new Scanner(fileSC.next());
                while (lineSC.hasNext()) {
                    String in = lineSC.next();
                    addTask(in, line / 4 * 100 + line % 4 * 15,
                        (line + 1) / 4 * 100 + (line + 1) % 4 * 15);
                }
                line++;
                lineSC.close();
            }
            fileSC.close();
        }
        catch (Exception e) {
            // ignore
        } 
    }

    public static void writeFile() {
        try {
            FileWriter myWriter = new FileWriter(directory + "\\scheduledata_" + zonedDate + ".txt");
            String out = "";
            for (int j = 0; j < schedule.length - 1; j++) {
                out += "\n";
                if (schedule[j] != null) {
                    for (int i = 0; i < schedule[j].size(); i++) {
                        if (i > 0)
                            out += " ";
                        out += schedule[j].get(i);
                    }
                }
            }
            myWriter.write(out);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printHelp() {
        System.out.println("Choose from one of the following commands:");
        System.out.println("view [v]");
        System.out.println("add  [a]");
        System.out.println("del  [d]");
        System.out.println("move [m]");
        System.out.println("date [t]");
        System.out.println("list [l]");
        System.out.println("help [h]");
        System.out.println("exit [x]");
        System.out.println();
    }
        
    public static void printList() {
        String list = "";
        File dir = new File(directory);
        File[] directoryListing = dir.listFiles();
        Arrays.sort(directoryListing);
        if (directoryListing != null) {
            for (int i = 0; i < directoryListing.length; i++) {
                ArrayList<String> tasks = new ArrayList<String>();
                String date = directoryListing[i].getName().substring(13,23);
                if (!LocalDate.parse(date, myFormat).isBefore(ZonedDateTime.now(ZoneId.of(timeZone)).toLocalDate())) {
                    try {
                        Scanner fileSC = new Scanner(directoryListing[i]).useDelimiter("\n");
                        while (fileSC.hasNext()) {
                            Scanner lineSC = new Scanner(fileSC.next());
                            while (lineSC.hasNext()) {
                                String in = lineSC.next();
                                if (!tasks.contains(in))
                                    list += date + ": " + in + "\n";
                                    tasks.add(in);
                            }
                            lineSC.close();
                        }
                        fileSC.close();
                    }
                    catch (Exception e) {
                        System.out.println("Could not find schedule.");
                    } 
                    if (i < directoryListing.length - 1 && tasks.size() > 0)
                        list += "\n";
                }
            }
        } else {
            System.out.println("Could not find schedules.");
        }

        System.out.println(list);
    }

    public static void main(String[] args) {
        readFile();
        printHelp();
        
        Scanner sc1 = new Scanner(System.in).useDelimiter(System.lineSeparator());
        while (sc1.hasNext()) {
            String in1 = sc1.next();
            Scanner sc2 = new Scanner(in1);
            if (in1.toLowerCase().equals("exit") || in1.toLowerCase().equals("x")) {
                sc1.close();
                break;
            }
            
            while (true) {
                String in2 = sc2.next();
                if (in2.toLowerCase().equals("add") || in2.toLowerCase().equals("a")) {
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    
                    String in3 = sc2.next();
                    if (!sc2.hasNext()) {
                        addTask(in3, 2345, 2400);
                        System.out.println(in3 + " successfully added.");
                        System.out.println();
                        break;
                    }

                    String start = sc2.next();
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }

                    String end = sc2.next();
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    
                    try {
                        addTask(in3, Integer.parseInt(start), Integer.parseInt(end));
                        System.out.println(in3 + " successfully added.");
                    }
                    catch (Exception e) {
                        System.out.println(TIME_ERROR);
                    }

                    System.out.println();
                    break;
                }
                else if (in2.toLowerCase().equals("del") || in2.toLowerCase().equals("d")) {
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    String in3 = sc2.next();
                    if (!sc2.hasNext()) {
                        if (removeTask(in3)) {
                            System.out.println(in3 + " successfully deleted.");
                        }
                        else {
                            System.out.println(in3 + " not found.");
                        }
                        System.out.println();
                        break;
                    }
        
                    String in4 = sc2.next();
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }

                    try {
                        removeTask(Integer.parseInt(in3), Integer.parseInt(in4));
                        System.out.println("Time slot successfully deleted.");
                    }
                    catch (Exception e) {
                        System.out.println(TIME_ERROR);
                    }

                    System.out.println();
                    break;
                }
                else if (in2.toLowerCase().equals("view") || in2.toLowerCase().equals("v")) {
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    
                    printSchedule();
                    break;
                }
                else if (in2.toLowerCase().equals("help") || in2.toLowerCase().equals("h")) {
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    
                    printHelp();
                    break;
                }
                else if (in2.toLowerCase().equals("list") || in2.toLowerCase().equals("l")) {
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    
                    printList();
                    break;
                }
                else if (in2.toLowerCase().equals("date") || in2.toLowerCase().equals("t")) {
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    String in3 = sc2.next();
                    
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }
                    try {
                        if (in3.length() == 10) {
                            LocalDate.parse(in3, myFormat);
                            zonedDate = in3;
                        }
                        else if (in3.length() == 5) {
                            LocalDate.parse(in3 + zonedDate.substring(5), myFormat);
                            zonedDate = in3 + zonedDate.substring(5);
                        }
                        else if (in3.length() == 2) {
                            LocalDate.parse(zonedDate.substring(0, 3) + in3 + zonedDate.substring(5), myFormat);
                            zonedDate = zonedDate.substring(0, 3) + in3 + zonedDate.substring(5);
                        }
                        else {
                            System.out.println(DATE_ERROR);
                            System.out.println();
                            break;
                        }
                        readFile();
                        System.out.println("Date successfully changed to " + zonedDate + ".");
                        System.out.println();
                    }
                    catch(Exception e) {
                        System.out.println(DATE_ERROR);
                        System.out.println();
                        break;
                    }
                    break;
                }
                else if (in2.toLowerCase().equals("move") || in2.toLowerCase().equals("m")) {
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }

                    String in3 = sc2.next();
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }

                    String in4 = sc2.next();
                    if (!sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }

                    String in5 = sc2.next();
                    if (sc2.hasNext()) {
                        System.out.println(COMMAND_ERROR);
                        System.out.println();
                        break;
                    }

                    if (in4.equals("+")) {
                        try {
                            int s = Integer.parseInt(in5);
                            if (s < MIN_HOUR || s > MAX_HOUR_TIME || 
                                    s % 100 % 15 != 0 || s % 100 / 60 != 0) {
                                System.out.println(TIME_ERROR);
                                break;
                            }

                            if (shiftTask(in3, s)) {
                                System.out.println(in3 + " successfully moved.");
                            }
                            else {
                                System.out.println(in3 + " not found.");
                            }
                        }
                        catch (Exception e) {
                            System.out.println(TIME_ERROR);
                        }
                        
                        System.out.println();
                        break;
                    }
                    else if (in4.equals("-")) {
                        try {
                            int s = Integer.parseInt(in5);
                            if (s < MIN_HOUR || s > MAX_HOUR_TIME || 
                                    s % 100 % 15 != 0 || s % 100 / 60 != 0) {
                                System.out.println(TIME_ERROR);
                                break;
                            }
                            
                            if (shiftTask(in3, -s)) {
                                System.out.println(in3 + " successfully moved.");
                            }
                            else {
                                System.out.println(in3 + " not found.");
                            }
                        }
                        catch (Exception e) {
                            System.out.println(TIME_ERROR);
                        }

                        System.out.println();
                        break;
                    }  
                    else {
                        try {
                            addTask(in3, Integer.parseInt(in4), Integer.parseInt(in5));
                            removeTask(in3);
                            addTask(in3, Integer.parseInt(in4), Integer.parseInt(in5));
                            System.out.println(in3 + " successfully moved.");
                        }
                        catch (Exception e) {
                            System.out.println(in3 + " could not be moved.");
                        }

                        System.out.println();
                        break;
                    }
                }
                else {
                    System.out.println(COMMAND_ERROR);
                    System.out.println();
                    break;
                }
            }
            sc2.close();
        }
    }
}