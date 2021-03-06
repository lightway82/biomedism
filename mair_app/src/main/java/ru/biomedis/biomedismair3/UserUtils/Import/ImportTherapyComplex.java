package ru.biomedis.biomedismair3.UserUtils.Import;

import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ru.biomedis.biomedismair3.ModelDataApp;
import ru.biomedis.biomedismair3.entity.TherapyComplex;
import ru.biomedis.biomedismair3.utils.Text.TextUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



@Slf4j
public class ImportTherapyComplex
{


    private Listener listener=null;
    private List<Complex> complexes = new ArrayList();
    private List<Program> listProgram=new ArrayList<>();

    public ImportTherapyComplex()
    {
    }

    /**
     * Парсит файл структуры профиля и импортирует их
     * @param xmlFile xml файл
     * @param mda модель данных
     * @return true если все удачно
     */
    public int parse(File xmlFile, ModelDataApp mda, ru.biomedis.biomedismair3.entity.Profile profile) throws Exception {

        if (listener == null) throw new Exception("Нужно реализовать слушатель событий!");
        boolean res = false;
        if (xmlFile == null) return 0;


        SAXParserFactory factory = SAXParserFactory.newInstance();

        factory.setValidating(true);
        factory.setNamespaceAware(false);
        SAXParser parser = null;


        try {
            parser = factory.newSAXParser();
            parser.parse(xmlFile, new ParserHandler());

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            if (listener != null) listener.onError(false);
            listener = null;
            return 0;
        } catch (SAXException e) {


            log.error("",e);
            if (listener != null) {

                if (e.getCause() instanceof FileTypeMissMatch) {
                    log.error("SAXException");

                    listener.onError(true);
                } else listener.onError(false);
            }
            clear();
            listener = null;
            return 0;

        } catch (Exception e) {
            log.error("",e);
            if (listener != null) listener.onError(false);
            clear();
            listener = null;
            return 0;
        }



        //сначала спарсим в коллекцию, потом проверим валидность ,
        // те можно ли из этого построить базу,
        // только после этого строим базу. Это позволит избежать лишних ошибок при некорректных файлах
        if (listener != null) listener.onStartAnalize();
        //проверим нет ли частот которые не пропарсишь.

        try {
            String[] split = null;
            String[] split2 = null;

            for (Program itm : listProgram) {
                if(itm.freqs.isEmpty())continue;
                itm.freqs =  itm.freqs.replace(",",".");
                split = itm.freqs.split(";");
                for (String s : split) {
                    if (s.contains("+")) {
                        split2 = s.split("\\+");
                        for (String s1 : split2) Double.parseDouble(s1);
                    } else Double.parseDouble(s);


                }


            }
        } catch (NumberFormatException e) {

            log.error("Неверный формат частот",e);
            if (listener != null) listener.onError(true);
            listener = null;
            clear();
            return 0;
        }


        if (listener != null) listener.onEndAnalize();

        if (listener != null) listener.onStartImport();
        //если все хорошо можно импортировать объекты в базу

        int resSize;
        try {
            Iterator<Complex> it1;
            ImportTherapyComplex.Complex complex;
            for(it1 = this.complexes.iterator(); it1.hasNext(); complex.complex = mda.createTherapyComplex(complex.srcuuid,profile, complex.name, complex.descr, complex.timeForFreq,complex.bundlesLength)) {
                complex =it1.next();
            }

            Iterator<Program> it = this.listProgram.iterator();
            ImportTherapyComplex.Program prog;
            while(it.hasNext()) {
                prog = it.next();
                mda.createTherapyProgram(prog.srcuuid,this.complexes.get(prog.complexIndex).complex, prog.name, prog.descr, prog.freqs,prog.multy);
            }

            resSize = this.complexes.size();
        } catch (Exception ee) {
            log.error("Ошибка создание элементов базы", ee);
            if(this.listener != null) {
                this.listener.onError(true);
            }

            this.listener = null;

            try {
                Iterator<Complex> iterator = this.complexes.iterator();

                while(iterator.hasNext()) {
                    ImportTherapyComplex.Complex var30 = iterator.next();
                    if(var30.complex != null) {
                        mda.removeTherapyComplex(var30.complex);
                    }
                }
            } catch (Exception var22) {
                log.error("Не удалось откатить изменения", ee);
            }

            resSize = 0;
        }

        if(listener!=null)listener.onEndImport();



        if(this.listener != null) {
            this.listener.onEndImport();
        }

        if(this.listener != null) {
            if(resSize != 0) {
                this.listener.onSuccess();
            } else {
                this.listener.onError(false);
            }
        }

        this.listener = null;
        this.clear();
        return resSize;

    }

    private void clear()
    {

        this.complexes.forEach(i -> i.complex = null);
        this.listProgram.clear();

    }

    class ParserHandler extends DefaultHandler {
        boolean fileTypeOK = false;
        boolean inComplex = false;

        ParserHandler() {
        }

        public void startDocument() throws SAXException {
            super.startDocument();
            if(ImportTherapyComplex.this.listener != null) {
                ImportTherapyComplex.this.listener.onStartParse();
            }

        }

        public void endDocument() throws SAXException {
            super.endDocument();
            if(ImportTherapyComplex.this.listener != null) {
                ImportTherapyComplex.this.listener.onEndParse();
            }

        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if(qName.equals("UserComplexes")) {
                this.fileTypeOK = true;
                super.startElement(uri, localName, qName, attributes);
            } else {
                SAXException saxException;
                if(!this.fileTypeOK) {
                    saxException = new SAXException(ImportTherapyComplex.this.new FileTypeMissMatch());
                    throw saxException;
                } else if(qName.equals("Complex")) {
                    this.inComplex = true;

                    if(attributes.getLength() != 0) {

                        String srcuuid = attributes.getValue("srcuuid");
                        if (srcuuid == null) srcuuid = "";

                        int bundles= Integer.parseInt(attributes.getValue("bundlesLength")==null?"3":attributes.getValue("bundlesLength"));
                        ImportTherapyComplex.this.complexes.add(ImportTherapyComplex.this.new Complex(TextUtil.unEscapeXML(attributes.getValue("name")),
                                TextUtil.unEscapeXML(attributes.getValue("description")),
                                Integer.parseInt(attributes.getValue("timeForFreq")),
                                bundles<2?3:bundles,srcuuid));
                    }

                    super.startElement(uri, localName, qName, attributes);
                } else if(qName.equals("Program")) {
                    if(!ImportTherapyComplex.this.complexes.isEmpty() && this.inComplex) {
                        if(attributes.getLength() != 0) {

                            boolean multy;
                            String multy_s = attributes.getValue("multy");
                            if(multy_s==null)multy=true;
                            else multy=Boolean.valueOf(multy_s);

                            String srcuuid = attributes.getValue("srcuuid");
                                if (srcuuid == null) srcuuid = "";


                            ImportTherapyComplex.this.listProgram.add(ImportTherapyComplex.this.new Program(TextUtil.unEscapeXML(attributes.getValue("name")),
                                    TextUtil.unEscapeXML(attributes.getValue("description")),
                                    attributes.getValue("frequencies"),
                                    ImportTherapyComplex.this.complexes.size() - 1,
                                    multy,srcuuid));
                        }

                        super.startElement(uri, localName, qName, attributes);
                    } else {
                        saxException = new SAXException(ImportTherapyComplex.this.new FileTypeMissMatch());
                        throw saxException;
                    }
                } else if(this.fileTypeOK) {
                    saxException = new SAXException(ImportTherapyComplex.this.new FileTypeMissMatch());
                    throw saxException;
                } else {
                    super.startElement(uri, localName, qName, attributes);
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if(qName.equals("Complex")) {
                this.inComplex = false;
            }

            super.endElement(uri, localName, qName);
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            super.characters(ch, start, length);
        }
    }


    public void setListener(Listener listener){this.listener=listener;}

    public interface Listener
    {
        public void onStartParse();
        public void onEndParse();
        public void onStartAnalize();
        public void onEndAnalize();
        public void onStartImport();
        public void onEndImport();
        public void onSuccess();

        /**
         *
         * @param fileTypeMissMatch false просто ошибка парсинга, true тип файла неверный
         */
        public void onError(boolean fileTypeMissMatch);

    }





    class Program {
        String name;
        String descr;
        String freqs;
        boolean multy;
        int complexIndex;
        ru.biomedis.biomedismair3.entity.Program program;
        String srcuuid;

        public Program(String name, String descr, String freqs, int complexIndex,  boolean multy,String srcuuid) {
            this.name = name;
            this.descr = descr;
            this.freqs = freqs;
            this.complexIndex = complexIndex;
            this.multy=multy;
            this.srcuuid = srcuuid;
        }
    }

    class Complex {
        String name;
        String descr;

        int timeForFreq;
        TherapyComplex complex;
        int bundlesLength=1;
        String srcuuid;

        public Complex(String name, String descr, int timeForFreq,Integer bundlesLength,String srcuuid) {
            this.name = name;
            this.descr = descr;

            this.timeForFreq = timeForFreq;
            this.srcuuid = srcuuid;
            if(bundlesLength!=null) this.bundlesLength=bundlesLength;

        }
    }

    /**
     * Тип файла не соответствует типу обработчика. Должен содержать тег UserProfile
     */
    class FileTypeMissMatch extends Exception
    {


    }
}
