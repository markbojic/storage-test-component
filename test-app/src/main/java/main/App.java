package main;

import java.util.Scanner;
import java.util.Set;
import org.reflections.Reflections;
import models.User;
import specs.DirectoryManipulation;
import specs.FileManipulation;

public class App {

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static void main(String[] args) {

		System.out.println("------------------------------------");
		System.out.println("    Welcome to Storage Manager");
		System.out.println("------------------------------------");
		System.out.print("Enter storage link: ");
		Scanner sc = new Scanner(System.in);
		String storagePath = sc.nextLine();

		DirectoryManipulation dm = null;
		FileManipulation fm = null;

		User user = null;
		boolean accessGranted = false;
		while (!accessGranted) {
			System.out.print("Enter username: ");
			String username = sc.nextLine();
			System.out.print("Enter password: ");
			String password = sc.nextLine();
			user = new User(username, password);

			Reflections reflections = new Reflections("models");
			Set<Class<? extends DirectoryManipulation>> subTypes = reflections
					.getSubTypesOf(DirectoryManipulation.class);
			Class<? extends DirectoryManipulation> implementation = ((Class<? extends DirectoryManipulation>) subTypes
					.toArray()[0]);
			System.out.println(implementation.getSimpleName());
			try {
				dm = implementation.newInstance();// Set to first class found that implements DirectoryManipulation
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}

			// Find the implementation for the FileManipulation class
			Set<Class<? extends FileManipulation>> subTypes2 = reflections.getSubTypesOf(FileManipulation.class);
			Class<? extends FileManipulation> implementation2 = (Class<? extends FileManipulation>) subTypes2
					.toArray()[0];
			try {
				fm = implementation2.newInstance();// Set to first class found that implements FileManipulation
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
			System.out.println(implementation2.getSimpleName());

			// Initiate storage with DirectoryClass

			dm.initStorage(storagePath, user);
			if (dm.getRoot() == null) {
				System.out.println("Storage connection failed - Wrong Username or Password!");
			} else {
				fm.setForbiddenExtensions(dm.getForbiddenExtensions());
				fm.setRoot(dm.getRoot());
				accessGranted = true;
			}
		}

		// Console parsing

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
				System.out.println("rmf file_path     -     [Delete File]");
				System.out.println("upf file_path destination_path     -     [Upload File]");
				System.out.println(
						"upmf file1_path file2_path... destination_path archive_name     -     [Upload Multiple Files]");
				System.out.println("dlf file_path destination_path     -     [Download File]");

				System.out.println("4 - Directory Operations:");
				System.out.println("mkdir dir_name destination_path     -     [Create Directory]");
				System.out.println("mkdir {number} dir_name destination_path     -     [Create {number} Directories");
				System.out.println("rmdir dir_path     -     [Delete Directory]");
				System.out.println("updir dir_path destination_path     -     [Upload Directory]");
				System.out.println("updir-z dir_path destination_path     -     [Upload Zipped Directory");
				System.out.println("dldir dir_path destination_path     -     [Download Directory]");
				System.out.println("ls-a dir_path     -     [List All Files]");
				System.out.println("ls dir_path     -     [List Files]");
				System.out.println("ls-s dir_path extension     -     [List Files With Given Extension]");
				System.out.println("ls-d dir_path     -     [List Directories]");
				System.out.println(
						"If wanting to perform any of the oprations above on the ROOT path, ommit the path from the command and it will automatically be set to the current online storage path! THIS IS SPECIFIC FOR WORKING WITH A REMOTE STORAGE!!!");

				continue;
			}
			// ako nema spejs a nije lsusr onda je greska
			if (!command.contains(" ") && !command.equals("lsusr") && !command.equals("ls-a")
					&& !command.equals("ls")) {
				System.out.println("Wrong input!");
				continue;
			}
			if (command.equals("lsusr")) {
				user.listAllUsers(dm.getRoot());
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
					fm.createFile(mkfparts[1], "", user);
				else
					fm.createFile(mkfparts[1], mkfparts[2], user);
				break;
			case "rmf":
				// rmf file_path
				// Delete File (file_path, user)
				String rmfpath = command.substring(command.indexOf(" ") + 1);
				fm.deleteFile(rmfpath, user);
				break;
			case "upf":
				// upf file_path dest_path
				// Upload File (file_path, dest_path, user)
				String[] upfparts = command.split(" ");
				if (upfparts[2].equalsIgnoreCase("root"))
					fm.uploadFile(upfparts[1], "", user);
				else
					fm.uploadFile(upfparts[1], upfparts[2], user);
				break;
			case "dlf":
				// dlf file_path dest_path
				// Download File (file_path, dest_path, user)
				String dlfparts[] = command.split(" ");
				fm.downloadFile(dlfparts[1], dlfparts[2], user);
				break;
			case "mkdir":
				// mkdir name dest_path
				// Create Directory (name, dest_path, user)
				String[] mkdirparts = command.split(" ");
				if (mkdirparts.length == 3) {
					if (mkdirparts[2].equalsIgnoreCase("root"))
						dm.createDirectory(mkdirparts[1], "", user);
					else
						dm.createDirectory(mkdirparts[1], mkdirparts[2], user);
				} else if (mkdirparts.length == 4) {
					String numb = mkdirparts[1].substring(1, mkdirparts[1].length() - 1);
					int n = Integer.parseInt(numb);
					if (mkdirparts[3].equalsIgnoreCase("root")) {
						for (int i = 1; i <= n; i++) {
							dm.createDirectory(mkdirparts[2] + i, "", user);
						}
					} else {
						for (int i = 1; i <= n; i++) {
							dm.createDirectory(mkdirparts[2] + i, mkdirparts[3], user);
						}
					}
				}
				break;
			case "rmdir":
				// rmdir path
				// Delete Directory (path, user)
				String rmdirpath = command.substring(command.indexOf(" ") + 1);
				dm.deleteDirectory(rmdirpath, user);
				break;
			case "updir":
				// updir dir_path dest_path
				// Upload Directory (dir_path, dest_path, user)
				String[] updirparts = command.split(" ");
				if (updirparts[2].equalsIgnoreCase("root"))
					dm.uploadDirectory(updirparts[1], "", user);
				else
					dm.uploadDirectory(updirparts[1], updirparts[2], user);
				break;
			case "dldir":
				// dldir dir_path dest_path
				// Download Directory (dir_path, dest_path, user)
				String[] dldirparts = command.split(" ");
				dm.downloadDirectory(dldirparts[1], dldirparts[2], user);
				break;
			case "ls-a":
				// ls-a path
				// List All Files (path)
				String lsapath = command.substring(command.indexOf(" ") + 1);
				if (lsapath.equalsIgnoreCase("root"))
					dm.listAllFiles("");
				else
					dm.listAllFiles(lsapath);
				break;
			case "ls":
				// lf path
				// List Files (path, "all")
				String[] lsparts = command.split(" ");
				if (lsparts[1].equalsIgnoreCase("root"))
					dm.listFiles("", lsparts[2]);
				else
					dm.listFiles(lsparts[1], lsparts[2]);

				break;
			case "ls-s":
				// lf path extension
				// List Files With Given Extension (path, extension)
				String lssparts[] = command.split(" ");
				if (lssparts[1].equalsIgnoreCase("root"))
					dm.listFiles("", lssparts[2]);
				else
					dm.listFiles(lssparts[1], lssparts[2]);
				break;
			case "ls-d":
				// ls-d path
				// List Directories (path)
				// String lsdpath = command.substring(command.indexOf(" ") + 1);
				String lsdparts[] = command.split(" ");
				if (lsdparts[1].equalsIgnoreCase("root"))
					dm.listDirectories("");
				else
					dm.listDirectories(lsdparts[1]);
				break;
			case "mkusr":
				// mkusr username password priv1 priv2 priv3 priv4
				// Create User (username, password, privs[], root, "")
				String[] parts = command.split(" ");
				boolean[] privs = { parts[3].equals("true") ? true : false, parts[4].equals("true") ? true : false,
						parts[5].equals("true") ? true : false, parts[6].equals("true") ? true : false };
				user.createUser(parts[1], parts[2], privs, dm.getRoot());
				break;
			case "rmusr":
				// rmusr username
				// Delete User (username, root, "")
				String usrnm = command.substring(command.indexOf(" ") + 1);
				user.deleteUser(usrnm, dm.getRoot());
				break;

			case "updir-z":
				// Upload directory as .zip
				String[] updirzparts = command.split(" ");
				if (updirzparts[2].equalsIgnoreCase("root"))
					dm.uploadZipDirectory(updirzparts[1], "", user);
				else
					dm.uploadZipDirectory(updirzparts[1], updirzparts[2], user);
				break;
			case "upmf":
				String upmfparts[] = command.split(" ");
				String[] multipleFiles = new String[upmfparts.length - 3];
				int j = 0;
				for (int i = 1; i < upmfparts.length - 2; i++) {
					multipleFiles[j] = upmfparts[i];
					j++;
				}
				if (upmfparts[upmfparts.length - 2].equalsIgnoreCase("root"))
					fm.uploadMultipleFilesZip(multipleFiles, "", upmfparts[upmfparts.length - 1], user);
				else
					fm.uploadMultipleFilesZip(multipleFiles, upmfparts[upmfparts.length - 2],
							upmfparts[upmfparts.length - 1], user);
				break;
			default:
				System.out.println("Wrong input! Command does not exist...");
			}
		}
		System.out.println("Exiting...");
		sc.close();

	}

}
