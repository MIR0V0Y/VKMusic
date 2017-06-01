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

	public static void main(String[] args) throws IOException, UnsupportedTagException, InvalidDataException {
		
		// Ищем папку с кэшем
		File cachdir = new File(System.getProperty("user.home"));
		if (System.getProperty("os.name").indexOf("Windows") != -1){
			cachdir = new File(System.getProperty("user.home") + "\\AppData\\Local\\Mozilla\\Firefox\\Profiles");
			cachdir = new File(cachdir.listFiles()[0] + "\\cache2\\entries");
			if (!cachdir.exists()){
				System.out.println("Папка с кэшем не найдена. Выход");
				return;
			}
		} else{
			cachdir = new File(System.getProperty("user.home") + "/.cache/mozilla/firefox");
			cachdir = new File(cachdir.listFiles()[0] + "/cache2/entries");
			if (!cachdir.exists()){
				System.out.println("Папка с кэшем не найдена. Выход");
				return;
			}
		}
		
		// Получаем от пользователя папку сохранения
		File savedir;
		if (args.length==1){
			savedir = new File(args[0]);
			if (!savedir.exists()){
				System.out.println("Создание новой папки...");
				if (!savedir.mkdir()){
					System.out.println("Ошибка создания новой папки, проверьте корректность имени папки");
					return;
					}
			}
			System.out.println("Папка сохранения : "+savedir.getAbsolutePath());
		}else{
			System.out.println("Неправильно указаны параметры работы программы. Выход");
			return;
		}
		
		System.out.println();
		
		// Перебираем все файлы, кроме самых коротких
		File[] files = cachdir.listFiles();
		int count = 0;
		for (int i=0; i < files.length; i++){
			File f = files[i];
			if (f.length()/(1024*1024)<1)	// Чем меньше размер файла, тем быстрее идет поиск + отсеиваются сторонние звуки (оповещение в VK)
				continue;
			
			// Рабочий сет каждого файла
			byte[] bs = new byte[1000];
			char[] c = new char[1000];
			
			// Чтение из файла
			try(FileInputStream fis = new FileInputStream(f.getPath())){
				fis.skip(f.length()-1000);
				fis.read(bs);
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
			for (int j = 0; j<bs.length; j++)
				c[j] = (char)bs[j];
			String head =new String(c);
			
			
			if (head.indexOf("audio/") != -1){
				System.out.println("Найден аудио файл: "+f.getName());
				count++;
				// Генерируем имя файла из ID3 тегов
				String name = new String(f.getName());
				Mp3File mp3file = new Mp3File(f.getPath());
				if (mp3file.hasId3v1Tag()) {
				  ID3v1 id3v1Tag = mp3file.getId3v1Tag();
				  name = id3v1Tag.getArtist() + " - " + id3v1Tag.getTitle();
				}
				if (mp3file.hasId3v2Tag()) {
					  ID3v2 id3v2Tag = mp3file.getId3v2Tag();
					  name = id3v2Tag.getArtist()+ " - " +id3v2Tag.getTitle();
				}
				if (!mp3file.hasId3v2Tag() && !mp3file.hasId3v1Tag()){
					System.out.println("Аудиозапись повреждена, не удалось получить название трека");
					name = "Broken - "+f.getName();
				}
				
				// Сохранение mp3 в папку
				File newFile = new File(savedir.getPath() +"/"+ name +".mp3");
				if (newFile.exists()){
					if (f.length() == newFile.length()){
						count--;
						continue; // Файл уже добавлен в коллекцию
					}else{
						newFile = new File(savedir.getPath() +"/"+ name +" Copy.mp3"); // в кэше нашлась другая версия этой же мелодии
						if(newFile.exists()){
							count--;
							continue; // Copy-файл уже добавлен в коллекцию
						}
					}
				}
				FileChannel sourceChannel = new FileInputStream(f.getPath()).getChannel();
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
				
			}
			
		}

		System.out.println();
		System.out.println("Сохранено "+count+" новых аудио файлов");
		
	}

}



