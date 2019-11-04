package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;

import models.DirectoryLocalImplementation;
import models.DirectoryRemoteManipulation;
import models.FileLocalImplementation;
import users.User;

public class App {
	
	public App () {
		
	}

	public static void main(String[] args) {
		System.out.println("------------------------------------");
		System.out.println("Welcome to Storage Manager");
		System.out.println("------------------------------------");
		System.out.print("Enter storage link: ");
		Scanner sc = new Scanner(System.in);
		String storagePath = sc.nextLine();
		// proveri da li je path za dropbox(remore) ili za local
		String storageType = null;
		if (storagePath.contains("dropbox")) {
			storageType = "remote";
		} else {
			storageType = "local";
			if (!Files.exists(Paths.get(storagePath.substring(0, storagePath.lastIndexOf(File.separator))))) {
				System.out.println("ERROR!");
				// ne postoji zadati path
			}
		}
		// uzme username i password
		System.out.print("Enter username: ");
		String username = sc.nextLine();
		System.out.print("Enter password: ");
		String password = sc.nextLine();
		// init local storage ( 1. za postojace 2. za novo)
		if (storageType.equals("local")) {
			DirectoryLocalImplementation localSt = new DirectoryLocalImplementation();
			FileLocalImplementation localF = new FileLocalImplementation();
			User user = null;
			
			if (Files.exists(Paths.get(storagePath))) {
				// 1. existing local storage
				
				// log in
				BufferedReader br;
				try {
					br = new BufferedReader(new FileReader(new File(storagePath + "\\accounts.log")));
					String line = br.readLine();
					while (line != null) {
						String[] splitter = line.split("/");
						if (splitter[0].equalsIgnoreCase(username) && splitter[1].equals(password)) {
							boolean priv1 = splitter[2].equals("true")?true:false;
							boolean priv2 = splitter[3].equals("true")?true:false;
							boolean priv3 = splitter[4].equals("true")?true:false;
							boolean priv4 = splitter[5].equals("true")?true:false;
							boolean[] niz = {priv1, priv2, priv3, priv4};
							user = new User(username, password, niz);
							break;
						}
						line = br.readLine();
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
				if (user == null) {
					System.out.println("Failed to login...");
				} else {
					System.out.println("Logged in successfully...");
					// check if user is admin
					File file = new File(storagePath + "\\storage-info.txt");
					try {
						Stream<String> lines = Files.lines(file.toPath());
						if ((lines).anyMatch(line -> line.contains(username))) {
							user.setAdmin(true);
						}
						lines.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					// set root
					localSt.setRoot(storagePath);
					// set forbidden extensions
					BufferedReader br2;
					try {
						br2 = new BufferedReader(new FileReader(new File(storagePath + "\\storage-info.txt")));
						br2.readLine(); // preskoci prvu liniju
						String line = br2.readLine();
						String extensions = "";
						while (line != null) {
							//System.out.println(line);
							extensions = extensions + " " + line;
							line = br2.readLine();
						}
						String[] splitExts = extensions.split(" ");
						localF.setForbiddenExtensions(splitExts);
						
					} catch(IOException ex) {
						ex.printStackTrace();
					}
				}
				
			} else {
				// 2. init new local storage
				boolean[] niz = { (true), (true), (true), (true) }; // posto je kreator(admin) sve mu je dozvoljeno
				user = new User(username, password, niz);
				
				System.out.println("Set forbidden extensions (separate with spaces): ");
				String extensionsStr = sc.nextLine();
				String[] extensionArray = extensionsStr.split(" ");
				localF.setForbiddenExtensions(extensionArray);
				
				localSt.initStorage(storagePath, extensionArray, user);
			}
			
			// hc test start
			boolean[] tmp1 = { (false), (false), (false), (true) };
			user.createUser("korisnik1", "pswrd", tmp1, localSt.getRoot());
			user.createUser("korisnik2", "12345", tmp1, localSt.getRoot());
			user.listAllUsers(localSt.getRoot());
			
			localSt.createDirectory("novi folder", localSt.getRoot(), user);
			// hc test end
			
			String command = "";
			while (!command.equals("exit")) {
				// TODO ifovi za sve komande
				System.out.print("Command: ");
				command = sc.nextLine();
			}
			System.out.println("Exiting...");
			
		} else {
			// TODO remote initStorage
			DirectoryRemoteManipulation remoteSt = new DirectoryRemoteManipulation();
			System.out.println("radi remote........");
		}
	}
	
}
