package ru.biomedis.biomedismair3.utils.Files;

import ru.biomedis.biomedismair3.Log;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Создание директорий, удаление директорий в папке profiles
 * Created by Anama on 13.10.2015.
 */
public class FilesProfileHelper
{


    /**
     * Возвратит список программа в папке комплекса, распознает автоматически старый и новый тип
     * @param dir
     * @return вернет null в случае ошибки
     */
    public static  Map<Long, ProgramFileData> getProgrammsFromComplexDir(File dir)
    {
        Map<Long, ProgramFileData> programms=null;
        try {
            programms = FilesProfileHelper.getProgramms(dir);
        } catch (OldComplexTypeException e)
        {
            try {
                programms = FilesOldProfileHelper.getProgramms(dir, 300);
            } catch (Exception e1) {
                return null;
            }
        }catch (Exception e){
            Log.logger.error("",e);return null;}
        return programms;
    }


    /**
     * Список директорий комплексов
     * @param diskPath путь к диску
     * @return
     */
    public static List<ComplexFileData> getComplexes(File diskPath) throws OldComplexTypeException {

        List<ComplexFileData> res=new ArrayList<>();
        // до конца рекурсивного цикла
        if (!diskPath.exists())  return null;
        File textFile=null;

        for (File file : diskPath.listFiles( dir -> dir.isDirectory()))
        {
            //найдем первый попавшийся текстовый файл
            textFile=null;

            for (File file1 : file.listFiles((dir, name) -> name.contains(".txt")))
            {
                textFile=file1;
                break;
            }

            //пустая папка
            if(textFile==null)
            {
                res.add(new ComplexFileData(-1, file.getName(), 0,false, file));
                continue;
            }
            //прочитаем файл

            List<String> progrParam = new ArrayList<String>();
            try( Scanner in = new Scanner(textFile,"UTF-8"))
            {
                while (in.hasNextLine()) progrParam.add(in.nextLine().replace("@",""));
            }catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
            if(progrParam.size()!=8) throw new OldComplexTypeException("Обнаружен комплекс старого формата",textFile.getName().substring(textFile.getName().length()));

            res.add(new ComplexFileData(Long.parseLong(progrParam.get(2)), file.getName(), Long.parseLong(progrParam.get(3)),
                    Boolean.parseBoolean(progrParam.get(4)), file));

        }

        return res;
    }


    /**
     * Программы выбранного комплекса из директории
     * @param complexDir
     * @return
     */
    public static Map<Long,ProgramFileData> getProgramms(File complexDir) throws OldComplexTypeException {
        Map<Long,ProgramFileData> res=new LinkedHashMap<>();

        if (!complexDir.exists())  return null;

        //найдем bss без текста и удалим
        for (File file : complexDir.listFiles(dir -> dir.isFile()&&dir.getName().contains(".bss")))
        {
            File txtFile=new File(complexDir,file.getName().substring(0,file.getName().length()-4)+".txt");
            if(!txtFile.exists())file.delete();
        }


        for (File file : complexDir.listFiles(dir -> dir.isFile()&&dir.getName().contains(".txt")))
        {


            List<String> progrParam = new ArrayList<String>();
            try( Scanner in = new Scanner(file,"UTF-8"))
            {
                while (in.hasNextLine()) progrParam.add(in.nextLine().replace("@",""));
            }catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
            if(progrParam.size()!=8) throw new OldComplexTypeException("Обнаружен комплекс старого формата",complexDir.getName().substring(0,file.getName().length()));

        File bssFile=new File(complexDir,file.getName().substring(0,file.getName().length()-4)+".bss");

            res.put(Long.parseLong(progrParam.get(0)), new ProgramFileData
                    (
                            Long.parseLong(progrParam.get(0)),
                            Long.parseLong(progrParam.get(2)),
                            Long.parseLong(progrParam.get(3)),
                            progrParam.get(1),
                            progrParam.get(6),
                            progrParam.get(5),
                            file,
                            bssFile.exists() ? bssFile : null, Boolean.parseBoolean(progrParam.get(7))));
        }

        return res;

    }




    /**
     * Создаст текстовый файл с описанием программы
     * @param freqs
     * @param timeForFreq
     * @param idProgram
     * @param txtPath
     *  @param uuid
     *  @param mp3  mp3 программа
     * @throws Exception
     */
    public static void copyTxt(String freqs,int timeForFreq,long idProgram, String uuid,long idComplex, boolean mullty,String nameProgram, boolean mp3,File txtPath) throws Exception {
        txtPath=new File(txtPath.toURI());

        try(PrintWriter writer = new PrintWriter(txtPath,"UTF-8"))
        {

            writer.println(idProgram);
            writer.println(uuid);
            writer.println(idComplex + "");
            writer.println(timeForFreq);
            writer.println(mullty?"true":"false");
            writer.println(nameProgram);
            writer.println(freqs+"@");
            writer.println(mp3?"true":"false");

        }catch (Exception e)
        {

           if(txtPath.exists()) txtPath.delete();

            throw new Exception(e);
        }

    }

    /**
     * Создаст папку  если ее нет
     * @param dstDir
     */
    public static Path copyDir(File dstDir) throws IOException {
        dstDir=new File(dstDir.toURI());//избавляет от проблемы с пробелами в путях
        if(!dstDir.exists())return Files.createDirectory(dstDir.toPath());
        else return dstDir.toPath();
    }

    /**
     * Просто копирует выбранный файл в указанный. Все названия и пути получаются из вне
     * @param srcFile
     * @param destFile
     * @throws Exception
     */
    public static void copyBSS(File srcFile,File destFile) throws Exception {

        destFile=new File(destFile.toURI());

        try(FileChannel source = new FileInputStream(srcFile).getChannel();FileChannel destination = new FileOutputStream(destFile).getChannel())
        {

            destination.transferFrom(source, 0, source.size());


        } catch (Exception e) {

            if(destFile.exists()) destFile.delete();
            throw new Exception(e);
        }

    }





    public static void recursiveLibsDelete(File diskPath) {

         recursiveDeleteLibsHelper(diskPath);

    }
    private static void recursiveDeleteLibsHelper(File path)
    {

        // до конца рекурсивного цикла
        if (!path.exists())
            return;

        //если это папка, то идем внутрь этой папки и удалим либ файлы и найдем папки, в них мы залезем
        if (path.isDirectory())
        {
            for (File f : path.listFiles((dir, name) -> name.contains(".lib")||name.contains(".LIB"))) f.delete();

            for (File f : path.listFiles((dir) -> dir.isDirectory()))  recursiveDeleteLibsHelper(f);

        }

    }


    public static boolean recursiveDelete(File diskPath) {

       return recursiveDeleteHelper(diskPath);

    }

    private static boolean recursiveDeleteHelper(File path)
    {

        // до конца рекурсивного цикла
        if (!path.exists())
            return false;

        //если это папка, то идем внутрь этой папки и вызываем рекурсивное удаление всего, что там есть
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                // рекурсивный вызов
                recursiveDeleteHelper(f);
            }
        }
        // вызываем метод delete() для удаления файлов и пустых(!) папок
        return path.delete();
    }


    private static boolean recursiveDeleteHelper(File path,String excludeNameInRoot,int tier)
    {

        // до конца рекурсивного цикла
        if (!path.exists())
            return false;

        //если это папка, то идем внутрь этой папки и вызываем рекурсивное удаление всего, что там есть
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                // рекурсивный вызов
                recursiveDeleteHelper(f,excludeNameInRoot,tier+1);
            }
        }
        // вызываем метод delete() для удаления файлов и пустых(!) папок
        if(!( tier==1 &&  excludeNameInRoot.equalsIgnoreCase(path.getName())))return path.delete();
       else return false;


    }

    public static boolean recursiveDelete(File diskPath,String excludeNameInRoot) {
        return recursiveDeleteHelper(diskPath,excludeNameInRoot,0);

    }
}