package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.stream.Stream;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import models.DirectoryLocalImplementation;
import models.DirectoryRemoteManipulation;
import models.FileLocalImplementation;
import models.FileRemoteManipulation;
import users.User;

public class App {

	private static final String ACCESS_TOKEN = "Gtb2Dvk8yKAAAAAAAAAAFLHGpRNZA0DT2z1NikOvWDJISMvOzJaSg48W2vzUQ1UI";

	public App() {

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
							boolean priv1 = splitter[2].equals("true") ? true : false;
							boolean priv2 = splitter[3].equals("true") ? true : false;
							boolean priv3 = splitter[4].equals("true") ? true : false;
							boolean priv4 = splitter[5].equals("true") ? true : false;
							boolean[] niz = { priv1, priv2, priv3, priv4 };
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
							// System.out.println(line);
							extensions = extensions + " " + line;
							line = br2.readLine();
						}
						String[] splitExts = extensions.split(" ");
						localF.setForbiddenExtensions(splitExts);

					} catch (IOException ex) {
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
			// boolean[] tmp1 = { (false), (false), (false), (true) };
			// user.createUser("korisnik1", "pswrd", tmp1, localSt.getRoot(), "");
			// user.createUser("korisnik2", "12345", tmp1, localSt.getRoot(), "");
			// user.listAllUsers(localSt.getRoot(), "");
			// localSt.createDirectory("novi folder", localSt.getRoot(), user);
			// hc test end

			// komunikacija preko komandne linije
			String command = "";
			while (true) {
				System.out.print("Command: ");
				command = sc.nextLine();
				if (command.equals("exit"))
					break;
				if (command.equals("help")) {
					System.out.println("1 - General Commands:");
					System.out.println("exit     -     [Exit the program.]");
					System.out.println("help     -     [Displays all commands.]");

					System.out.println("2 - User Operations:");
					System.out.println("mkusr username password priv1 priv2 priv3 priv4     -     [Create User]");
					System.out.println("rmusr username     -     [Delete User]");
					System.out.println("lsusr     -     [List All Users]");

					System.out.println("3 - File Operations:");
					System.out.println("mkf file_name destination_path     -     [Create File]");
					System.out.println("rmf file_name     -     [Delete File]");
					System.out.println("upf file_path destination_path     -     [Upload File]");
					System.out.println(
							"upmf file1_path file2_path... destination_path archive_name     -     [Upload Multiple Files]");
					System.out.println("dlf file_path destination_path     -     [Download File]");

					System.out.println("4 - Directory Operations:");
					System.out.println("mkdir dir_name destination_path     -     [Create Directory]");
					System.out
							.println("mkdir {number} dir_name destination_path     -     [Create {number} Directories");
					System.out.println("rmdir dir_path     -     [Delete Directory]");
					System.out.println("updir dir_path destination_path     -     [Upload Directory]");
					System.out.println("updir-z dir_path destination_path     -     [Upload Zipped Directory");
					System.out.println("dldir dir_path destination_path     -     [Download Directory]");
					System.out.println("ls-a dir_path     -     [List All Files]");
					System.out.println("ls dir_path     -     [List Files]");
					System.out.println("ls-s dir_path extension     -     [List Files With Given Extension]");
					System.out.println("ls-d dir_path     -     [List Directories]");

					continue;
				}
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
				// System.out.println(prefix);
				switch (prefix) {
				case "mkf":
					// mkf file_name destination_path
					// Create File (name, dest_path, user)
					String[] mkfparts = command.split(" ");
					localF.createFile(mkfparts[1], localSt.getRoot() + mkfparts[2], user);
					break;
				case "rmf":
					// rmf file_path
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
				case "upmf":
					// upmf file1_path file2_path... destination_path archive_name
					// Upload Multiple Files
					String[] mfparts = command.split(" ");
					String[] multipleFiles = new String[mfparts.length - 3];
					int j = 0;
					for (int i = 1; i < mfparts.length - 2; i++) {
						multipleFiles[j] = mfparts[i];
						j++;
					}
					// System.out.println(localSt.getRoot() + mfparts[mfparts.length-2]);
					// System.out.println(mfparts[mfparts.length-1]);
					localF.uploadMultipleFilesZip(multipleFiles, localSt.getRoot() + mfparts[mfparts.length - 2],
							mfparts[mfparts.length - 1], user);
					break;
				case "dlf":
					// dlf file_path dest_path
					// Download File (file_path, dest_path, user)
					String dlfparts[] = command.split(" ");
					localF.downloadFile(localSt.getRoot() + dlfparts[1], dlfparts[2], user);
					break;
				case "mkdir":
					// mkdir name dest_path
					// mkdir {number} name dest_path
					// Create Directory (name, dest_path, user)
					String[] mkdirparts = command.split(" ");
					if (mkdirparts.length == 3) {
						localSt.createDirectory(mkdirparts[1], localSt.getRoot() + mkdirparts[2], user);
					} else {
						String numb = mkdirparts[1].substring(1, mkdirparts[1].length() - 1);
						// System.out.println(numb);
						int n = Integer.parseInt(numb);
						for (int i = 1; i <= n; i++) {
							localSt.createDirectory(mkdirparts[2] + i, localSt.getRoot() + mkdirparts[3], user);
						}
					}
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
				case "updir-z":
					// updir-z dir_path dest_path
					// Upload Zipped Directory (dir_path, dest_path, user)
					String[] updirzparts = command.split(" ");
					localSt.uploadZipDirectory(updirzparts[1], localSt.getRoot() + updirzparts[2], user);
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
					// ls path
					// List Files (path, "all")
					String lspath = command.substring(command.indexOf(" ") + 1);
					localSt.listFiles(localSt.getRoot() + lspath, "all");
					break;
				case "ls-s":
					// ls path extension
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
					boolean[] privs = { parts[3].equals("true") ? true : false, parts[4].equals("true") ? true : false,
							parts[5].equals("true") ? true : false, parts[6].equals("true") ? true : false };
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

			// Parsiranje linka u konkretan naziv foldera na dropboxu
			String[] splitter1 = storagePath.split("/");
			String storageName = splitter1[splitter1.length - 1];
			storageName = storageName.replace("%20", " ");
			User user = null;
			// System.out.println("radi remote........");

			// initRemoteStorage
			DirectoryRemoteManipulation remoteSt = new DirectoryRemoteManipulation();// Treba da se postavi root
			FileRemoteManipulation remoteF = new FileRemoteManipulation();// Postavka root-a i zabranjenih ekstenzija

			if (storageExists(storageName))
			// Ako postoji storage
			{
				// Log in - prolazak korz accounts file i provera da li se poklapa password i
				// username
				DbxRequestConfig config = DbxRequestConfig.newBuilder("testAccoutns").build();
				DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
				ListFolderResult result;
				File c = new File("src" + "/" + "accounts.log");
				try {
					c.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				OutputStream outputStream;
				try {
					result = client.files().listFolder("/" + storageName);
					while (true) {
						for (Metadata metadata : result.getEntries()) {
							if (metadata instanceof FileMetadata && metadata.getName().equals("accounts.log")) {
								outputStream = new FileOutputStream(c);
								metadata = client.files().downloadBuilder("/" + storageName + "/accounts.log")
										.download(outputStream);
								outputStream.close();
							}
						}
						break;
					}
					BufferedReader reader;
					try {
						reader = new BufferedReader(new FileReader(c.getAbsoluteFile()));
						String line = reader.readLine();
						while (line != null) {
							String splitter[] = line.split("/");
							if (splitter[0].equalsIgnoreCase(username)) {
								if (splitter[1].contentEquals(password)) {
									System.out.println("Logged in as :" + username);
									boolean priv1 = splitter[2].equals("true") ? true : false;
									boolean priv2 = splitter[3].equals("true") ? true : false;
									boolean priv3 = splitter[4].equals("true") ? true : false;
									boolean priv4 = splitter[5].equals("true") ? true : false;
									boolean[] niz = { priv1, priv2, priv3, priv4 };
									user = new User(username, password, niz);
									remoteSt.setRoot("/" + storageName);// Postavi root da bude taj storage
									System.out.println("Remote root set to : " + remoteSt.getRoot());
									remoteF.setRoot("/" + storageName);
									reader.close();
									break;
								} else
									System.out.println("Login failed!");
							}
							line = reader.readLine();
						}
						reader.close();
						PrintWriter writer;
						writer = new PrintWriter(c);
						writer.print("");
						writer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				File c1 = new File("src" + "/" + "storage-info.txt");
				try {
					c1.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				try {
					result = client.files().listFolder("/" + storageName);
					while (true) {
						for (Metadata metadata : result.getEntries()) {
							if (metadata instanceof FileMetadata && metadata.getName().equals("storage-info.txt")) {
								outputStream = new FileOutputStream(c1);
								metadata = client.files().downloadBuilder("/" + storageName + "/storage-info.txt")
										.download(outputStream);
								outputStream.close();
							}
						}
						break;
					}
					BufferedReader reader;
					try {
						reader = new BufferedReader(new FileReader(c1.getAbsoluteFile()));
						String line = reader.readLine();
						while (line != null) {
							if (line.trim().equalsIgnoreCase(username)) {
								user.setAdmin(true);
							}
							line = reader.readLine();
						}
						reader.close();

					} catch (IOException e) {
						e.printStackTrace();
					}
					BufferedReader br2;
					try {
						br2 = new BufferedReader(new FileReader(c1.getAbsoluteFile()));
						br2.readLine(); // preskoci prvu liniju
						String line = br2.readLine();
						String extensions = "";
						while (line != null) {
							extensions = extensions + " " + line;
							System.out.println("FE -> " + line);
							line = br2.readLine();
						}
						String[] splitExts = extensions.split(" ");
						remoteF.setForbiddenExtensions(splitExts);// Postavi ekstenzije
						br2.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}

					// Files.deleteIfExists(Paths.get(c1.getAbsolutePath()));
					PrintWriter writer;
					try {
						writer = new PrintWriter(c1);
						writer.print("");
						writer.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				// Pravi novi storage
				System.out.println("Storage does not exist, enter the forbidden extension for the new storage: ");
				boolean p[] = { true, true, true, true };
				user = new User(username, password, p);
				user.setAdmin(true);
				String extensionsStr = sc.nextLine();
				String[] extensionArray = extensionsStr.split(" ");
				remoteSt.initStorage(storageName, extensionArray, user);
				remoteF.setForbiddenExtensions(extensionArray);
				remoteF.setRoot(remoteSt.getRoot());
				System.out.println("New storage root set to " + remoteSt.getRoot());
			}

			// System.out.println("Kreni parsiranje za remote");
			// remoteF.createFile("novi.txt", "", user);
			// remoteSt.createDirectory("Dir", "", user);
			// remoteF.createFile("jos.txt", "Dir", user);//Omoguci da ispravi ako je unet
			// FileSeparator pre path-a
			// remoteF.uploadFile("C:/New Folder/OutputFile.json", "Dir", user);
			// remoteSt.uploadDirectory("C:/New folder 1", "", user);
			String command = "";
			while (true) {
				System.out.print("Command: ");
				command = sc.nextLine();
				if (command.equals("exit"))
					break;
				if (command.equals("help")) {
					System.out.println("1 - General Commands:");
					System.out.println("exit     -     [Exit the program.]");
					System.out.println("help     -     [Displays all commands.]");

					System.out.println("2 - User Operations:");
					System.out.println("mkusr username password priv1 priv2 priv3 priv4     -     [Create User]");
					System.out.println("rmusr username     -     [Delete User]");
					System.out.println("lsusr     -     [List All Users]");

					System.out.println("3 - File Operations:");
					System.out.println("mkf file_name destination_path     -     [Create File]");
					System.out.println("rmf file_name     -     [Delete File]");
					System.out.println("upf file_path destination_path     -     [Upload File]");
					System.out.println(
							"upmf file1_path file2_path... destination_path archive_name     -     [Upload Multiple Files]");
					System.out.println("dlf file_path destination_path     -     [Download File]");

					System.out.println("4 - Directory Operations:");
					System.out.println("mkdir dir_name destination_path     -     [Create Directory]");
					System.out
							.println("mkdir {number} dir_name destination_path     -     [Create {number} Directories");
					System.out.println("rmdir dir_path     -     [Delete Directory]");
					System.out.println("updir dir_path destination_path     -     [Upload Directory]");
					System.out.println("updir-z dir_path destination_path     -     [Upload Zipped Directory");
					System.out.println("dldir dir_path destination_path     -     [Download Directory]");
					System.out.println("ls-a dir_path     -     [List All Files]");
					System.out.println("ls dir_path     -     [List Files]");
					System.out.println("ls-s dir_path extension     -     [List Files With Given Extension]");
					System.out.println("ls-d dir_path     -     [List Directories]");
					System.out.println(
							"If wanting to perform any of the oprations above on the ROOT path, ommit the path from the command and it will automatically be set to the current online storage path!");

					continue;
				}
				// ako nema spejs a nije lsusr onda je greska
				if (!command.contains(" ") && !command.equals("lsusr") && !command.equals("ls-a")
						&& !command.equals("ls")) {
					System.out.println("Wrong input!");
					continue;
				}
				if (command.equals("lsusr")) {
					// ovo je jedina komanda bez spejsa
					user.listAllUsers(remoteSt.getRoot(), ACCESS_TOKEN);
					continue;
				}
				String prefix = command.substring(0, command.indexOf(" "));
				// System.out.println(prefix);
				switch (prefix) {
				case "mkf":
					// mkf file_name destination_path
					// Create File (name, dest_path, user)
					String[] mkfparts = new String[3];
					mkfparts = command.split(" ");
					if (mkfparts[2].equalsIgnoreCase("root"))
						remoteF.createFile(mkfparts[1], "", user);
					else
						remoteF.createFile(mkfparts[1], mkfparts[2], user);
					break;
				case "rmf":
					// rm file_path
					// Delete File (file_path, user)
					String rmfpath = command.substring(command.indexOf(" ") + 1);
					remoteF.deleteFile(rmfpath, user);
					break;
				case "upf":
					// upf file_path dest_path
					// Upload File (file_path, dest_path, user)
					String[] upfparts = command.split(" ");
					if (upfparts[2].equalsIgnoreCase("root"))
						remoteF.uploadFile(upfparts[1], "", user);
					else
						remoteF.uploadFile(upfparts[1], upfparts[2], user);
					break;
				case "dlf":
					// dlf file_path dest_path
					// Download File (file_path, dest_path, user)
					String dlfparts[] = command.split(" ");
					remoteF.downloadFile(dlfparts[1], dlfparts[2], user);
					break;
				case "mkdir":
					// mkdir name dest_path
					// Create Directory (name, dest_path, user)
					String[] mkdirparts = command.split(" ");
					if (mkdirparts.length == 3) {
						if (mkdirparts[2].equalsIgnoreCase("root"))
							remoteSt.createDirectory(mkdirparts[1], "", user);
						else
							remoteSt.createDirectory(mkdirparts[1], mkdirparts[2], user);
					} else if (mkdirparts.length == 4) {
						String numb = mkdirparts[1].substring(1, mkdirparts[1].length() - 1);
						int n = Integer.parseInt(numb);
						if (mkdirparts[3].equalsIgnoreCase("root")) {
							for (int i = 1; i <= n; i++) {
								remoteSt.createDirectory(mkdirparts[2] + i, "", user);
							}
						} else {
							for (int i = 1; i <= n; i++) {
								remoteSt.createDirectory(mkdirparts[2] + i, mkdirparts[3], user);
							}
						}
					}
					break;
				case "rmdir":
					// rmdir path
					// Delete Directory (path, user)
					String rmdirpath = command.substring(command.indexOf(" ") + 1);
					remoteSt.deleteDirectory(rmdirpath, user);
					break;
				case "updir":
					// updir dir_path dest_path
					// Upload Directory (dir_path, dest_path, user)
					String[] updirparts = command.split(" ");
					if (updirparts[2].equalsIgnoreCase("root"))
						remoteSt.uploadDirectory(updirparts[1], "", user);
					else
						remoteSt.uploadDirectory(updirparts[1], updirparts[2], user);
					break;
				case "dldir":
					// dldir dir_path dest_path
					// Download Directory (dir_path, dest_path, user)
					String[] dldirparts = command.split(" ");
					remoteSt.downloadDirectory(dldirparts[1], dldirparts[2], user);
					break;
				case "ls-a":
					// ls-a path
					// List All Files (path)
					String lsapath = command.substring(command.indexOf(" ") + 1);
					if (lsapath.equalsIgnoreCase("root"))
						remoteSt.listAllFiles("");
					else
						remoteSt.listAllFiles(lsapath);
					break;
				case "ls":
					// lf path
					// List Files (path, "all")
					String[] lsparts = command.split(" ");
					if (lsparts[2].equalsIgnoreCase("root"))
						remoteSt.listFiles("", lsparts[2]);
					else
						remoteSt.listFiles(lsparts[1], lsparts[2]);

					break;
				case "ls-s":
					// lf path extension
					// List Files With Given Extension (path, extension)
					String lssparts[] = command.split(" ");
					if (lssparts[1].equalsIgnoreCase("root"))
						remoteSt.listFiles("", lssparts[2]);
					else
						remoteSt.listFiles(lssparts[1], lssparts[2]);
					break;
				case "ls-d":
					// ls-d path
					// List Directories (path)
					// String lsdpath = command.substring(command.indexOf(" ") + 1);
					String lsdparts[] = command.split(" ");
					if (lsdparts[1].equalsIgnoreCase("root"))
						remoteSt.listDirectories("");
					else
						remoteSt.listDirectories(lsdparts[1]);
					break;
				case "mkusr":
					// mkusr username password priv1 priv2 priv3 priv4
					// Create User (username, password, privs[], root, "")
					String[] parts = command.split(" ");
					boolean[] privs = { parts[3].equals("true") ? true : false, parts[4].equals("true") ? true : false,
							parts[5].equals("true") ? true : false, parts[6].equals("true") ? true : false };
					user.createUser(parts[1], parts[2], privs, remoteSt.getRoot(), ACCESS_TOKEN);
					break;
				case "rmusr":
					// rmusr username
					// Delete User (username, root, "")
					String usrnm = command.substring(command.indexOf(" ") + 1);
					user.deleteUser(usrnm, remoteSt.getRoot(), ACCESS_TOKEN);
					break;

				case "updir-z":
					// Upload directory as .zip
					String[] updirzparts = command.split(" ");
					if (updirzparts[2].equalsIgnoreCase("root"))
						remoteSt.uploadZipDirectory(updirzparts[1], "", user);
					else
						remoteSt.uploadZipDirectory(updirzparts[1], updirzparts[2], user);
					break;
				case "upmf":
					String upmfparts[] = command.split(" ");
					String[] multipleFiles = new String[upmfparts.length - 3];
					int j = 0;
					for (int i = 1; i < upmfparts.length - 2; i++) {
						multipleFiles[j] = upmfparts[i];
						j++;
					}
					if(upmfparts[upmfparts.length - 2].equalsIgnoreCase("root"))
						remoteF.uploadMultipleFilesZip(multipleFiles, "", upmfparts[upmfparts.length -1], user);
					else
						remoteF.uploadMultipleFilesZip(multipleFiles, upmfparts[upmfparts.length -2], upmfparts[upmfparts.length -1], user);
					break;
				default:
					System.out.println("Wrong input! Command does not exist...");
				}
			}
			System.out.println("Exiting...");
			sc.close();
		}
	}

	private static boolean storageExists(String storageName) {
		DbxRequestConfig config = DbxRequestConfig.newBuilder("test").build();
		DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);
		ListFolderResult result;
		try {
			result = client.files().listFolder("");
			while (true) {
				for (Metadata metadata : result.getEntries()) {
					if (metadata instanceof FolderMetadata && metadata.getName().equals(storageName)) {
						System.out.println("Storage with name : " + storageName + " exists on dropbox.");
						return true;
					}
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
