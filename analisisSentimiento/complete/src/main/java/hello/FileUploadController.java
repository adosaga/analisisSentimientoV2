package hello;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hello.storage.StorageFileNotFoundException;
import hello.storage.StorageService;

@Controller
public class FileUploadController {

	private final StorageService storageService;
    public String ruta="vacio";
	@Autowired
	public FileUploadController(StorageService storageService) {
		this.storageService = storageService;
	}

	@GetMapping("/")
	public String listUploadedFiles(Model model) throws IOException {

		model.addAttribute("files", storageService.loadAll().map(
				path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
						"serveFile", path.getFileName().toString()).build().toString())
				.collect(Collectors.toList()));

		return "uploadForm";
	}

	@GetMapping("/files/{filename:.+}")
	@ResponseBody
	public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

		Resource file = storageService.loadAsResource(filename);
		ruta = "antes: "+filename+file.getFilename();
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + file.getFilename() + "\"").body(file);
	}

	@PostMapping("/")
	public String handleFileUpload(@RequestParam("file") MultipartFile file,
			RedirectAttributes redirectAttributes) throws IOException {

		storageService.store(file);
        //file.
        ruta="C:\\Users\\DELL\\eclipse-workspace\\analisisSentimiento\\complete\\target\\upload-dir\\";
        File archi=new File(ruta+file.getOriginalFilename());
        //file.transferTo(archi);
        /////////////Cargando el lexico/////////////////////////////////////////////////////////////////////
        File lexico=new File("C:\\lexico_afinn.csv");
        FileReader readerL = new FileReader(lexico);
        BufferedReader brL = new BufferedReader(readerL);
        String lineL;
        lineL = brL.readLine();
        List<Lexico> lexicoLista=new ArrayList<Lexico>();
        while ((lineL = brL.readLine()) != null) {
        	Lexico a=new Lexico();
        	String[] lexicoPal = lineL.split(",");
        	String original =lexicoPal[0]; 
			String cadenaNormalize = Normalizer.normalize(original, Normalizer.Form.NFD);   
			String cadenaSinAcentos = cadenaNormalize.replaceAll("[^\\p{ASCII}]", "");
			lexicoPal[0]=cadenaSinAcentos;
			lexicoPal[0]=lexicoPal[0].trim();
        	a.palabra = lexicoPal[0];
        	a.ponderacion = Integer.parseInt(lexicoPal[1]);
        	lexicoLista.add(a);
        }
        brL.close();
        readerL.close();        
        //////////////////////////////////////////////////////////////////////////////////
        try (FileReader reader = new FileReader(archi);
               BufferedReader br = new BufferedReader(reader)) {
        	   String alfabeto = "abcdefghijklmnñopqrstuvwxyz0123456789";
               String line;
               line = br.readLine();
               int numlin = 1;
               Writer archivoSalida=new BufferedWriter(new FileWriter(ruta+"Limpiado"+file.getOriginalFilename()));
               Writer archivoSalidaSenti=new BufferedWriter(new FileWriter(ruta+"ScriptsSentimiento"+file.getOriginalFilename()));
               Writer archivoSalidaLex=new BufferedWriter(new FileWriter(ruta+"ScriptsLexico"+file.getOriginalFilename()));
               while ((line = br.readLine()) != null) {
            	   try {
            	   String[] columnas = line.split("\\|");
            	   if (columnas[0].equals("AFINN")) {
            		   columnas[19]=columnas[19].toLowerCase();
            		   String original =columnas[19]; 
        			   String cadenaNormalize = Normalizer.normalize(original, Normalizer.Form.NFD);   
        			   String cadenaSinAcentos = cadenaNormalize.replaceAll("[^\\p{ASCII}]", "");
        			   columnas[19] = cadenaSinAcentos;
            		   String sinCambios= columnas[19];
            		   for(int j=0;j<columnas[19].length();j++) 
        				   if(alfabeto.indexOf(columnas[19].substring(j,j+1))<0) //es un carácter no válido{
        					   sinCambios=sinCambios.replace(columnas[19].substring(j,j+1), "|");
            		   columnas[19]=sinCambios;
            		   String palabras[] = columnas[19].split("\\|");//texto depurado
            		   String linea="";
            		   if(columnas[14].isBlank())columnas[14]="0";
            		   if(columnas[15].isBlank())columnas[15]="0";            		   
            		   for(int i=0;i<palabras.length;i++) {
            			   palabras[i]=palabras[i].trim();
            			   if(palabras[i].length()>1)
            			   for(int j=0;j<lexicoLista.size();j++)
            			   if(lexicoLista.get(j).palabra.equals(palabras[i])){
            				   linea+=palabras[i]+","+lexicoLista.get(j).ponderacion+",";
            				   archivoSalidaLex.append("insert into lexico values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','"+palabras[i]+"',"+lexicoLista.get(j).ponderacion+");\n");            				   
            				   break;
            			   }
            		   }
            		   if(!columnas[21].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','anger',"+columnas[21]+");\n");
            		   if(!columnas[22].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','fear',"+columnas[22]+");\n"); 
            		   if(!columnas[23].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','joy',"+columnas[23]+");\n"); 
            		   if(!columnas[24].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','sadness',"+columnas[24]+");\n"); 
            		   if(!columnas[25].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','analitycal',"+columnas[25]+");\n"); 
            		   if(!columnas[26].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','confident',"+columnas[26]+");\n");
            		   if(!columnas[27].isBlank())archivoSalidaSenti.append("insert into sentimiento values('"+columnas[28]+"',"+numlin+","+columnas[14]+","+columnas[15]+",'"+columnas[16]+"','tentative',"+columnas[27]+");\n"); 
            		   archivoSalida.append(line+"|"+linea+"\n");
            		   numlin++;
            	   }
            	   else {
            		   //archivoSalida.append("analizado "+line);
            		   String cadOri = columnas[2]; 
            		   columnas[2]=columnas[2].replace("RT ", "");
            		   int maximo=0;
            		   while(columnas[2].indexOf("#")>=0 & maximo<=10) {
            			   int posHashtag = columnas[2].indexOf("#");
            			   int primerEspacio = columnas[2].substring(posHashtag).indexOf(" ")+
            				   					columnas[2].substring(0,posHashtag).length();
            			   if (primerEspacio<posHashtag)primerEspacio=posHashtag;
            			   if (primerEspacio>=0)
            				   columnas[2]=columnas[2].replace(columnas[2].substring(posHashtag,primerEspacio),"");
            			   else
            				   columnas[2]=columnas[2].replace(columnas[2].substring(posHashtag),"");
            			   maximo++;
            			   if(maximo==10)
            				   break;
            		   }
            		   /////////////////////////////////////////////////////////////////////
            		   maximo=0;
            		   while(columnas[2].indexOf("@")>=0 & maximo<=10) {
            			   int posHashtag = columnas[2].indexOf("@");
            			   int primerEspacio = columnas[2].substring(posHashtag).indexOf(" ")+
            				   					columnas[2].substring(0,posHashtag).length();
            			   if (primerEspacio<posHashtag)primerEspacio=posHashtag;
            			   if (primerEspacio>=0)
            				   columnas[2]=columnas[2].replace(columnas[2].substring(posHashtag,primerEspacio),"");
            			   else
            				   columnas[2]=columnas[2].replace(columnas[2].substring(posHashtag),"");
            			   maximo++;
            			   if(maximo==10)
            				   break;
            		   }
            	   //	///////////////////////////////////////////////////////////////////////////
            		   maximo=0;
            		   while(columnas[2].indexOf("http")>=0 & maximo<=10) {
            			   int posHashtag = columnas[2].indexOf("http");
            			   int primerEspacio = columnas[2].substring(posHashtag).indexOf(" ")+
            				   					columnas[2].substring(0,posHashtag).length();
            			   if (primerEspacio<posHashtag)primerEspacio=posHashtag;
            			   if (primerEspacio>=0)
            				   columnas[2]=columnas[2].replace(columnas[2].substring(posHashtag,primerEspacio),"");
            			   else
            				   columnas[2]=columnas[2].replace(columnas[2].substring(posHashtag),"");
            			   maximo++;
            			   if(maximo==10)
            				   break;
            		   }            	   
            	   //	////////////////////////////////////////////////////////////////////////////
            		   columnas[2]=columnas[2].replace("  ", " ");
            		   columnas[2]=columnas[2].replace("  ", " "); 
            	   //	////////////////////////////////////////////////////////////////////////////
            		   RestTemplate restTemplate = new RestTemplate();
            		   String call = "https://translation.googleapis.com/language/translate/v2?target=en&key=xxxxxxxxx&q=";
            		   call += columnas[2];
            		   GTranslate trad = restTemplate.getForObject(call, GTranslate.class);
            		   String traduccion = trad.data.translations.get(0).translatedText;
            	   //	/////////////////////////////////////////////////////////////////////////////////////////
            	   //	call = "https://translation.googleapis.com/language/translate/v2?target=en&key=AIzaSyCh5lZljqpShNTaz-UxCiSCsn6elk2cMxc&q=";
            	   //	call += columnas[2];
            		   TextoAnalisis texto=new TextoAnalisis();
            		   texto.text = traduccion;
            		   call = "https://gateway-wdc.watsonplatform.net/tone-analyzer/api/v3/tone?version=2019-10-06";
            		   restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(
            				   "apikey","xxxxxxxx"));
            		   AnalisisSentimiento respAnalisis = restTemplate.postForObject(call, texto, AnalisisSentimiento.class);
            		   String senti="";
            	   //	//////////Anger////////////////////////////////////
            		   String valorAnger="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++)
            			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("anger"))
            			   		valorAnger += respAnalisis.document_tone.tones.get(i).score;
            	   //	//////////Fear////////////////////////////////////
            		   String valorFear="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++)
                   			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("fear"))
                   			   		valorFear += respAnalisis.document_tone.tones.get(i).score;
           		   //	//////////Joy////////////////////////////////////
            		   String valorJoy="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++)
                   			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("joy")) 
                   			   		valorJoy += respAnalisis.document_tone.tones.get(i).score;
           		   //	//////////Sadness////////////////////////////////////
            		   String valorSadness="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++) 
                   			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("sadness"))
                   			   		valorSadness += respAnalisis.document_tone.tones.get(i).score;
           		   //	//////////Analytical////////////////////////////////////           		   
            		   String valorAnalytical="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++) 
                   			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("analytical"))
                   			   		valorAnalytical += respAnalisis.document_tone.tones.get(i).score;          		
        		   //	//////////Confident////////////////////////////////////           		   
            		   String valorConfident="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++)
                			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("confident"))
                				   //	valorConfident += respAnalisis.document_tone.tones.get(i).score;
                			   		valorConfident+= respAnalisis.document_tone.tones.get(i).score;  
        		   //	//////////Tentative////////////////////////////////////           		   
            		   String valorTentative="";
            		   for(int i=0;i<respAnalisis.document_tone.tones.size();i++) 
                			   	if(respAnalisis.document_tone.tones.get(i).tone_id.equals("tentative"))
                			   		valorTentative += respAnalisis.document_tone.tones.get(i).score;
            		   
            		   senti = valorAnger+"|"+valorFear+"|"+valorJoy+"|"+valorSadness+"|"+valorAnalytical+"|"+valorConfident+"|"+valorTentative;
            		   archivoSalida.append(line+"|"+columnas[2]+"|"+traduccion+"|"+senti+"\n");
            	   }
            	 }catch(Exception e)
            	   {
            		 archivoSalida.append(e.getMessage());
            	   }  
               }
               archivoSalida.close();
               archivoSalidaSenti.close();
               archivoSalidaLex.close();
               redirectAttributes.addFlashAttribute("message",
                       "1ra linea " + line + "!");
           }
		
		//redirectAttributes.addFlashAttribute("message",
				//"You successfully uploaded " + file.getOriginalFilename() + "!");

		return "redirect:/";
	}

	@ExceptionHandler(StorageFileNotFoundException.class)
	public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
		return ResponseEntity.notFound().build();
	}

}
