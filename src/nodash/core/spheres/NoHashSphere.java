/*
 * Copyright 2014 David Horscroft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * The NoHashSpehre stores database hashes for user verification.
 */

package nodash.core.spheres;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nodash.core.NoCore;
import nodash.exceptions.NoDashFatalException;
import nodash.models.NoUser;

public final class NoHashSphere {
	private static Set<String> database = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	
	@SuppressWarnings("unchecked")
	public static void setup() {
		if (NoCore.config.saveDatabase) {
			File file = new File(NoCore.config.databaseFilename);
			if (file.exists()) {
				try {
					byte[] data = Files.readAllBytes(file.toPath());
					ByteArrayInputStream bais = new ByteArrayInputStream(data);
					ObjectInputStream ois = new ObjectInputStream(bais);
					NoHashSphere.database = (Set<String>) ois.readObject();
					ois.close();
					bais.close();
				} catch (IOException e){
					throw new NoDashFatalException("Unable to load up given database file.");
				} catch (ClassNotFoundException e) {
					throw new NoDashFatalException("Database file not in a verifiable format.");
				}
			}
		}
	}
	
	public static synchronized void saveToFile() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(NoHashSphere.database);
		byte[] data = baos.toByteArray();
		oos.close();
		baos.close();
		File file = new File(NoCore.config.databaseFilename);
		Files.write(file.toPath(), data, StandardOpenOption.CREATE);
	}
	
	public static synchronized void addNewNoUser(NoUser user) throws IOException {
		String hash = user.createHashString();
		NoHashSphere.database.add(hash); 
		NoHashSphere.saveToFile();
	}
	
	public static synchronized void insertHash(String hash) throws IOException {
		NoHashSphere.database.add(hash); 
		NoHashSphere.saveToFile();
	}
	
	public static synchronized void removeHash(String hash) throws IOException {
		NoHashSphere.database.remove(hash);
		NoHashSphere.saveToFile();
	}
	
	public static synchronized boolean checkHash(String hash) {
		return NoHashSphere.database.contains(hash);
	}

	public static synchronized int size() {
		return NoHashSphere.database.size();
	}
}
