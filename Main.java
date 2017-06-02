/*
VKMusic
Copyright (C) 2017 Artem Novozhilov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

// mp3agic - http://github.com/mpatric/mp3agic
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

public class Main {
	
	static File savedir;
	static File cachdir;

	public static void main(String[] args) throws IOException, UnsupportedTagException, InvalidDataException {
		
		cachdir = findCache();
		if (cachdir == null)
			return;
		
		savedir = getSave(args);
		if (savedir == null)
			return;
		
		System.out.println();
		
		File[] files = cachdir.listFiles();
		int count = 0;
		for (int i=0; i < files.length; i++){
			File f = files[i];
			/* Чем больше ограничение на размер файла, тем быстрее идет поиск 
			 * + отсеиваются сторонние звуки (оповещение в VK)
			 */
			if (f.length()/(1024*1024)<1)	
				continue;
			
			byte[] bs = new byte[1000];
			char[] c = new char[1000];
			
			// Чтение из файла
			try(FileInputStream fis = new FileInputStream(f.getPath())){
				fis.skip(f.length()-1000);
				fis.read(bs);
			}
			for (int j = 0; j<bs.length; j++)
				c[j] = (char)bs[j];
			String head = new String(c);
			
			if (head.indexOf("audio/") != -1){
				System.out.println("Найден аудио файл: "+f.getName());
				// Генерируем имя файла из ID3 тегов и сохраненяем в папку
				if (saveSong(f, getName(f)))
					count++;
			}
		}
		System.out.println();
		System.out.println("Сохранено "+count+" новых аудио файлов");
	}
	
	/**
     * Функция поиска папки с кэшем
     * Стучиться к стандартному расположению папки
     * @return папку с кэшем или null если не нашел
     */
	static File findCache(){
		File dir;
		if (System.getProperty("os.name").indexOf("Windows") != -1){
			dir = new File(System.getProperty("user.home") + "\\AppData\\Local\\Mozilla\\Firefox\\Profiles");
			dir = new File(dir.listFiles()[0] + "\\cache2\\entries");
			if (!dir.exists()){
				System.out.println("Папка с кэшем не найдена. Выход");
				return null;
			}
		} else{
			dir = new File(System.getProperty("user.home") + "/.cache/mozilla/firefox");
			dir = new File(dir.listFiles()[0] + "/cache2/entries");
			if (!dir.exists()){
				System.out.println("Папка с кэшем не найдена. Выход");
				return null;
			}
		}
		return dir;
	}
	
	/**
     * Функция получения папки сохранения
     * @param args - агрументы командной строки
     * Связывается или создаёт указанную папку для сохранения музыки
     * Если папка не указана - сохраняет в музыкальную папку пользователя
     * @return папку с кэшем или null
     */
	static File getSave(String[] args){
		File dir;
		if (args.length==1){
			dir = new File(args[0]);
			if (!dir.exists()){
				System.out.println("Создание новой папки...");
				if (!dir.mkdir()){
					System.out.println("Ошибка создания новой папки, проверьте корректность имени папки");
					return null;
				}
			}
		}else{
			if (args.length == 0){
				dir = new File(System.getProperty("user.home")+File.separator+"Music");
				if (!dir.exists())
					dir = new File(System.getProperty("user.home")+File.separator+"Музыка");
				
				if (!dir.exists()){
					System.out.println("Не удалось найти папку для сохранения. Укажите папку явно");
					return null;
				}
			}
			else{
				System.out.println("Неправильно указаны параметры работы программы. Выход");
				return null;
			}
		}
		System.out.println("Папка сохранения : "+dir.getAbsolutePath());
		return dir;
	}
	
	/**
     * Функция сохранения файла
     * @param file - файл предназначеный для сохранения
     * @param name - имя файла
     * @return true - файл сохранён, false - файл уже присутствует в коллекции
	 * @throws IOException 
     */
	static Boolean saveSong(File file, String name) throws IOException{
		File newFile = new File(savedir.getPath() + file.separator + name +".mp3");
		if (newFile.exists()){
			if (file.length() == newFile.length())
				return false;
			else{
				newFile = new File(savedir.getPath() + file.separator + name +" Copy.mp3"); // в кэше нашлась другая версия этой же мелодии
				if(newFile.exists())
					return false;
			}
		}
		FileChannel sourceChannel = new FileInputStream(file.getPath()).getChannel();
        try {
            FileChannel destChannel = new FileOutputStream(newFile.getPath()).getChannel();
            try {
                destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            } finally {
                destChannel.close();
            }
        } finally {
            sourceChannel.close();
        }
		return true;
	}
	
	/**
     * Функция чтения ID3 тэгов
     * @param file - файл предназначеный для чтения
     * @return предлагаемое имя файла
	 * @throws IOException 
	 * @throws InvalidDataException 
	 * @throws UnsupportedTagException 
     */
	static String getName(File file) throws UnsupportedTagException, InvalidDataException, IOException{
		String name = new String(file.getName());
		Mp3File mp3file = new Mp3File(file.getPath());
		if (mp3file.hasId3v1Tag()) {
		  ID3v1 id3v1Tag = mp3file.getId3v1Tag();
		  name = id3v1Tag.getArtist() + " - " + id3v1Tag.getTitle();
		}
		if (mp3file.hasId3v2Tag()) {
			  ID3v2 id3v2Tag = mp3file.getId3v2Tag();
			  name = id3v2Tag.getArtist()+ " - " +id3v2Tag.getTitle();
		}
		name = validName(name);
		if ((!mp3file.hasId3v2Tag() && !mp3file.hasId3v1Tag()) || name == " - " || name == "null - null"){
			System.out.println("Аудиозапись повреждена, не удалось получить название трека");
			name = "Broken - "+file.getName();
		}
		return name;
	}
	
	/**
     * Функция обработки имени файла
     * Делает имя файла валидным для NTFS под Windows
     * @param name - предварительное имя файла
     * @return валидное имя файла
     */
	static String validName(String name){
		name = name.replace('\\', ' ');
		name = name.replace('/', ' ');
		name = name.replace('"', ' ');
		name = name.replace('*', ' ');
		name = name.replace('?', ' ');
		name = name.replace('<', ' ');
		name = name.replace('>', ' ');
		name = name.replace('|', ' ');
		name = name.replace(':', ' ');
		return name;
	}
	

	
}




