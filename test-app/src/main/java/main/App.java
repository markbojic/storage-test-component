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
import models.FileRemoteManipulation;
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
		// <----- DO OVDE JE SVE ISTO ----->
		
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
			user.createUser("korisnik1", "pswrd", tmp1, localSt.getRoot(), "");
			user.createUser("korisnik2", "12345", tmp1, localSt.getRoot(), "");
			user.listAllUsers(localSt.getRoot(), "");
			
			localSt.createDirectory("novi folder", localSt.getRoot(), user);
			// hc test end
			
			// komunikacija preko komandne linije
			String command = "";
			while (true) {
				System.out.print("Command: ");
				command = sc.nextLine();
				if (command.equals("exit")) break;
				// ako nema spejs a nije lsusr onda je greska
				if (!command.contains(" ") && !command.equals("lsusr")) {
					System.out.println("Wrong input!");
					continue;
				}
				if (command.equals("lsusr")) {
					// ovo je jedina komanda bez spejsa
					user.listAllUsers(localSt.getRoot(), "");
					continue;
				}
				String prefix = command.substring(0, command.indexOf(" "));
				//System.out.println(prefix);
				switch(prefix) {
					case "mkf":
						// mkf file_name destination_path
						// Create File (name, dest_path, user)
						String[] mkfparts = command.split(" ");
						localF.createFile(mkfparts[1], localSt.getRoot() + mkfparts[2], user);
						break;
					case "rmf":
						// rm file_path
						// Delete File (file_path, user)
						String rmfpath = command.substring(command.indexOf(" ") + 1);
						localF.deleteFile(localSt.getRoot() + rmfpath, user);
						break;
					case "upf":
						// upf file_path dest_path
						// Upload File (file_path, dest_path, user)
						String[] upfparts = command.split(" ");
						localF.uploadFile(upfparts[1], localSt.getRoot() + upfparts[2], user);
						break;
					case "dlf":
						// dlf file_path dest_path
						// Download File (file_path, dest_path, user)
						String dlfparts[] = command.split(" ");
						localF.downloadFile(localSt.getRoot() + dlfparts[1], dlfparts[2], user);
						break;
					case "mkdir":
						// mkdir name dest_path
						// Create Directory (name, dest_path, user)
						String[] mkdirparts = command.split(" ");
						localSt.createDirectory(mkdirparts[1], localSt.getRoot() + mkdirparts[2], user);
						break;
					case "rmdir":
						// rmdir path
						// Delete Directory (path, user)
						String rmdirpath = command.substring(command.indexOf(" ") + 1);
						localSt.deleteDirectory(localSt.getRoot() + rmdirpath, user);
						break;
					case "updir":
						// updir dir_path dest_path
						// Upload Directory (dir_path, dest_path, user)
						String[] updirparts = command.split(" ");
						localSt.uploadDirectory(updirparts[1], localSt.getRoot() + updirparts[2], user);
						break;
					case "dldir":
						// dldir dir_path dest_path
						// Download Directory (dir_path, dest_path, user)
						String[] dldirparts = command.split(" ");
						localSt.downloadDirectory(localSt.getRoot() + dldirparts[1], dldirparts[2], user);
						break;
					case "ls-a":
						// ls-a path
						// List All Files (path)
						String lsapath = command.substring(command.indexOf(" ") + 1);
						localSt.listAllFiles(localSt.getRoot() + lsapath);
						break;
					case "ls":
						// lf path
						// List Files (path, "all")
						String lspath = command.substring(command.indexOf(" ") + 1);
						localSt.listFiles(localSt.getRoot() + lspath, "all");
						break;
					case "ls-s":
						// lf path extension
						// List Files With Given Extension (path, extension)
						String lssparts[] = command.split(" ");
						localSt.listFiles(localSt.getRoot() + lssparts[1], lssparts[2]);
						break;
					case "ls-d":
						// ls-d path
						// List Directories (path)
						String lsdpath = command.substring(command.indexOf(" ") + 1);
						localSt.listDirectories(localSt.getRoot() + lsdpath);
						break;
					case "mkusr":
						// mkusr username password priv1 priv2 priv3 priv4
						// Create User (username, password, privs[], root, "")
						String[] parts = command.split(" ");
						boolean[] privs = {parts[3].equals("true")?true:false, parts[4].equals("true")?true:false, parts[5].equals("true")?true:false, parts[6].equals("true")?true:false};
						user.createUser(parts[1], parts[2], privs, localSt.getRoot(), "");
						break;
					case "rmusr":
						// rmusr username
						// Delete User (username, root, "")
						String usrnm = command.substring(command.indexOf(" ") + 1);
						user.deleteUser(usrnm, localSt.getRoot(), "");
						break;
					default:
						System.out.println("Wrong input! Command does not exist...");
				}
				
			}
			System.out.println("Exiting...");
			sc.close();
		} else {
			// REMOTE ODAVDE
			// TODO initRemoteStorage
			DirectoryRemoteManipulation remoteSt = new DirectoryRemoteManipulation();
			//FileRemoteManipulation remoteF = new FileRemoteManipulation();
			User user = null;
			System.out.println("radi remote........");
		}
	}
	
}
