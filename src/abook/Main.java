package abook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

class Book {
	private TreeMap<String, Contact> contacts = new TreeMap<String, Contact>();
	
	public Contact set(Contact c) {		
		return contacts.put(c.getName(), c);		
	}
	
	public Contact get(String name) {
		return contacts.get(name);
	}
	
	public Contact remove(String name) {
		return contacts.remove(name);
	}
	
	public void load(Reader r) {
		Gson g = new Gson();
		Contact[] all = g.fromJson(r, Contact[].class);
		for (Contact c : all) {
			this.set(c);
		}
	}
	
	public void store(Writer w) {
		Gson g = new Gson();
		g.toJson(contacts.values().toArray(), w);
	}
	
	public List<Contact> search(String name) {
		List<Contact> result = new LinkedList<Contact>();
		
		Map<String, Contact> range = contacts;
		String start = contacts.ceilingKey(name);
		if (start != null) {
			range = contacts.tailMap(start);
		}
		for (Entry<String, Contact> e : range.entrySet()) {
			if (!e.getValue().getName().startsWith(name)) {
				break;
			}
			result.add(e.getValue());
		}
		
		return result;
	}
	
	public Collection<Contact> getAll() {
		return contacts.values();
	}
}

class Contact {
	private String name;
	private String phone;
	private String email;
	
	protected Contact(String name, String phone, String email) {
		this.name = name;
		this.phone = phone;
		this.email = email;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPhone() {
		return phone;
	}
	
	public String getEmail() {
		return email;
	}	
	
	@Override
	public String toString() {	
		return "Name: " + name + "; Phone: " + phone + "; email: " + email;
	}
}

public class Main {
	
	interface ICommand {
		String getName();			
		boolean prepare(String cmdLine);
		void run();		
		String getHelp();
		String getConfirmation();
		boolean isModifier();
	}
	
	abstract class Command implements ICommand {
		private List<String> args;
	
		private void parseCommandLine(String line) {
			args = new LinkedList<String>();
			Pattern p = Pattern.compile("(:?\\\"(.+?[^\\\\])\\\")|(\\S+)");
			Matcher m = p.matcher(line);
			while (m.find()) {
				if (m.group(2) != null) {
					args.add(m.group(2));
				}
				if (m.group(3) != null) {
					args.add(m.group(3));
				}
			}
		}
		
		@Override
		public boolean prepare(String cmdLine) {
			parseCommandLine(cmdLine);
			return true;
		}
		
		@Override
		public String getConfirmation() {		
			return null;
		}		
		
		protected List<String> getArguments() {
			return args;
		}
		
		@Override
		public boolean isModifier() {		
			return false;
		}
	}
	
	class TestCommand extends Command {

		@Override
		public String getName() {
			return "test";
		}
		
		@Override
		public boolean prepare(String cmdLine) {
			super.prepare(cmdLine);
			System.out.println("prepare: " + cmdLine);
			
			if (this.getArguments().isEmpty()) {
				System.out.println("no arguments))");
				return false;
			}
			
			return true; 
		}
		
		@Override
		public String getConfirmation() {		
			return "confirm testing";
		}

		@Override
		public void run() {
			List<String> args = this.getArguments();
			System.out.print("arguments: ");
			for (String s : args) {			
				System.out.print(s + "; ");
			}
			System.out.println();
			System.out.println("run done)");
		}

		@Override
		public String getHelp() {
			return "test: command for testing";
		}
		
	}
	
	class SearchCommand extends Command {
		
		private String name;

		@Override
		public String getName() {
			return "search";
		}
		
		@Override
		public boolean prepare(String cmdLine) {
			super.prepare(cmdLine);
			if (this.getArguments().size() != 1) {
				System.out.println("invalid number of arguments, search string should be single argument");
				return false;
			}
			
			name = this.getArguments().get(0).trim();
			if (name.isEmpty()) {			
				System.out.println("argument, search string is empty");	
				return false;
			}		
			
			return true;
		}

		@Override
		public void run() {						
			List<Contact> list = book.search(name);
			if (list.size() == 0) {
				System.out.println("no contacts starting with '" + name + "'");
			}			
			for (Contact c : list) {
				System.out.println(c);
			}
			System.out.println("total " + list.size() + " contacts found");
		}

		@Override
		public String getHelp() { 
			return "search <starting part of the name>: searches for user";				
		}		
	}
	
	class AddCommand extends Command {
		
		private Contact contact;

		@Override
		public String getName() {
			return "add";
		}
		
		@Override
		public String getConfirmation() {
			String confirm = "You are about to add: " + contact + "\n";
			Contact old = book.get(contact.getName());
			if (old != null) {
				confirm += "Addition will replace: " + old + "\n";
			}
			return confirm + "confirm addition";						
		}
		
		@Override
		public boolean prepare(String cmdLine) {		
			super.prepare(cmdLine);
			if (this.getArguments().size() != 3) {		
				System.out.println("invalid number of arguments, see help for details");
				return false;
			}
			//Here we can check for name, phone, email, blah, blah
			contact = new Contact(
				this.getArguments().get(0), 
				this.getArguments().get(1),
				this.getArguments().get(2)
			);
			
			return true;
		}

		@Override
		public void run() {
			book.set(contact);			
		}
		
		@Override
		public boolean isModifier() {		
			return true;
		}

		@Override
		public String getHelp() {
			return "add <name> <phone> <mail>: add contact to book";
		}
		
	}
	
	class DeleteCommand extends Command {
		Contact contact;

		@Override
		public String getName() { 
			return "delete";
		}

		@Override
		public boolean prepare(String cmdLine) {
			super.prepare(cmdLine);		
			if (this.getArguments().size() != 1) {		
				System.out.println("invalid number of arguments, see help for details");
				return false;
			}
			String name = this.getArguments().get(0);
			
			contact = book.get(name);
			if (contact == null) {
				System.out.println("Contact '" + name + "' not found");
				return false;
			}

			return true;
		}
		
		@Override
		public String getConfirmation() {			
			return "You are going to delete: " + contact + "\nconfirm deletion";
		}

		@Override
		public void run() {
			book.remove(contact.getName());
		}
		
		@Override
		public boolean isModifier() {		
			return true;
		}

		@Override
		public String getHelp() {
			return "delete <contact name>: delete contact from book";
		}		
	}
		
	
	class HelpCommand extends Command {
		@Override
		public String getHelp() {
			return "help: prints this message";
		}

		@Override
		public String getName() {
			return "help";
		}

		@Override
		public void run() {
			System.out.println("Address book commands: ");
			for (Entry<String, ICommand> e : commands.entrySet()) {
				System.out.println(e.getValue().getHelp());
			}
		}		
	}
	
	class ListCommand extends Command {

		@Override
		public String getName() { 
			return "list";
		}

		@Override
		public void run() {
			Collection<Contact> list = book.getAll();
			for (Contact c : list) {
				System.out.println(c);
			}
			System.out.println("total " + list.size() + " contacts");
		}

		@Override
		public String getHelp() {
			return "list: lists all contacts in address book";
		}
		
	}
	
	class ExitCommand extends Command {
		@Override
		public String getName() { 
			return "exit";
		}

		@Override
		public void run() {
			System.out.println("exiting...");
			running = false;
		}

		@Override
		public String getHelp() { 
			return "exit: ends address book session";
		}		
		
		@Override
		public boolean isModifier() {		
			return true;
		}
	}
		
	private Book book;
	private boolean running = true;
	private Map<String, ICommand> commands = new TreeMap<String, ICommand>();
	
	public void addCommand(ICommand c) {
		commands.put(c.getName(), c);
	}
	
	public ICommand getCommand(String name) {
		return commands.get(name);
	}
	
	public String getUserInput() throws IOException {
		StringBuilder sb = new StringBuilder();
		do {
			sb.append((char)System.in.read());
		} while(System.in.available() != 0);
		return sb.toString().trim();
	}
	
	public void loadBook() throws IOException {
		BufferedReader reader = null;
		File f = new File("book.json");
		if (!f.exists()) {
			return;
		}
		
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream("book.json"))); 
			book.load(reader);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {					
				}
			}
		}
	}
	
	public void storeBook() throws IOException {
		BufferedWriter writer = null;	
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("book.json"))); 
			book.store(writer);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ex) {					
				}
			}
		}
	}
	
	public void init() throws IOException {
		book = new Book();
		this.loadBook();
		
		addCommand(new TestCommand());
		addCommand(new SearchCommand());
		addCommand(new HelpCommand());
		addCommand(new ExitCommand());
		addCommand(new AddCommand());
		addCommand(new DeleteCommand());
		addCommand(new ListCommand());
	}
	
	
	protected void runCommand(ICommand cmd, String line) {
		try {
			if (!cmd.prepare(line)) {
				return;
			}
			if (cmd.getConfirmation() != null) {
				System.out.print(cmd.getConfirmation() + "[y/n]");
				String res = getUserInput();
				if (!res.equals("y")) {				
					System.out.println("cancelled");
					return;
				}				
			}
			cmd.run();
			
			if (cmd.isModifier()) {
				storeBook();
			}
			
		} catch (Exception ex) {
			System.out.println("Command execution failed, error: " + ex.getMessage());
		} 
	}
	
	public void run() throws IOException {
		running = true;
		do {
			System.out.print(">");
			String input = getUserInput();
			if (input.isEmpty()) {
				continue;
			}
			
			int pos = input.indexOf(" ");
			String command = input;
			String line = "";
			if (pos != -1) {
				command = input.substring(0, pos);
				line = input.substring(pos + 1); 
			}
			
			ICommand cmd = getCommand(command);
			if (cmd == null) {
				System.out.println("Command '" + command + "' not supported, try help");
				continue;
			}
			runCommand(cmd, line);
			System.out.println();
			
		} while(running);
		
		storeBook();
	}
	
	public static void main(String[] args) {
		try {
			System.out.println("Mega address book)))\n");
			
			Main app = new Main();
			app.init();
			app.run();
			
			System.out.println("bye.");
		} catch (Exception ex) {
			System.out.println("something went terribly wrong: " + ex.getMessage());
		}
	}
}
