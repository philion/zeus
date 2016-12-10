package com.acmerocket.chariot.cli;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.acmerocket.chariot.core.DeviceException;
import com.acmerocket.chariot.core.DeviceLoader;
import com.acmerocket.chariot.core.DeviceSet;

public class ChariotShell {
    private static final Logger LOG = LoggerFactory.getLogger(ChariotShell.class);

    public static final String DEFAULT_PROMPT = "\nchariot> ";

    private static final Set<String> EXIT_COMMANDS;
    static {
        final SortedSet<String> ecmds = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        ecmds.addAll(Arrays.asList("exit", "done", "quit", "end", "fino"));
        EXIT_COMMANDS = Collections.unmodifiableSortedSet(ecmds);
    }
    
    private String prompt = DEFAULT_PROMPT;
    private final DeviceSet devices;
    
    public ChariotShell(DeviceSet arg) {
        this.devices = arg;
    }

    private void output(@Nonnull final String format, @Nonnull final Object... args) {
        System.out.print(format(format, args));
        System.out.flush();
    }
    
    public void run() {
        final Scanner scanner = new Scanner(System.in);
        output(this.prompt);

        while (scanner.hasNext()) {
        	
        	String input = scanner.nextLine().trim();
        	
        	// check empty
        	if (input == null || input.length() == 0) {
        		// display help
        		this.displayHelp();
        		break; // FIXME
        	}
        	
            String[] parameters = input.split("\\s+");
            String deviceName = parameters[0];
            
            if (EXIT_COMMANDS.contains(deviceName)) {
                //output("Exit command %s issued, exiting.", deviceName);
            	LOG.info("Exiting.");
            	break; // FIXME
            }
            
        	if (parameters.length == 1) {
        		try {
        			output("commands: %s", this.devices.getCommands(deviceName));
        		}
        		catch (DeviceException ex) {
	            	output("%s\n", ex.getMessage());
	            	output("valid: %s", ex.getValidInput());
	                output(this.prompt);

	                continue; // FIXME
        		}
        	}
        	else {
	            String command = parameters[1];
	            String[] opts = Arrays.copyOfRange(parameters, 2, parameters.length);
	            //LOG.info("## device={}, command={}, opts={}", deviceName, command, opts);
	            
	            try {
	            	String result = this.devices.sendCommand(deviceName, command, opts);
	            	output("%s %s: %s", deviceName, command, result);
	            }
	            catch (DeviceException ex) {
	            	output("%s\n", ex.getMessage());
	            	output("Valid input: %s", ex.getValidInput());
	                output(this.prompt);

	                continue; // FIXME
	            }
            }
            
            output(this.prompt);
        }
        scanner.close();
    }

    /**
	 * Show standard help, for the provided devices
	 */
	protected void displayHelp() {
        this.output("Valid devices are: %s", this.devices.getDeviceNames());
	}

	public static void main(String[] args) throws IOException {
		String toLoad = "living-room";
		if (args != null && args.length > 0) {
			toLoad = args[0];
		}
		
        DeviceLoader loader = new DeviceLoader();
        DeviceSet devices = loader.load(toLoad);
        ChariotShell shell = new ChariotShell(devices);
        
        if (args != null && args.length > 0) {
            shell.execute(args);
        }
        
        shell.run();
        
        System.exit(0);
    }

    private void execute(String[] args) throws UnsupportedEncodingException {
        String command = String.join(" ", args) + " quit\n";
        InputStream commandInput = new ByteArrayInputStream(command.getBytes("UTF-8"));
        InputStream oldInput = System.in;
        try {
            System.setIn(commandInput);
            //output("executing: %s", command);
        } 
        finally {
            System.setIn(oldInput);
        }
    }

    // (device-name|macro-name) (action) (params)
    // i.e. denon volume-up, denon power-on, appletv hulu, etc.
    // macro: (device-a power-on, device-b power-on, device-a input device-b,
    // device-a output device-c
}
