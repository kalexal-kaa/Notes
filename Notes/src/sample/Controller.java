package sample;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class Controller implements Initializable {
    @FXML
    private ListView<String> list1,list2;
    @FXML
    private TextArea text;
    @FXML
    private Label userLogin;
    
    private String passwordKey;
    private String userDirectoryPath;
    private final String separator;
    private final Toast toast;
    private final Cryptograph cryptograph;

    public Controller() {
        this.separator = System.getProperty("file.separator");
        this.cryptograph = new Cryptograph();
        this.toast=new Toast();
    }

    @FXML
    private void list1Action(){
        showNotes(userDirectoryPath+separator+list1.getSelectionModel().getSelectedItem());
        text.clear();
    }
    @FXML
    private void list2Action(){
        text.setText(cryptograph.decode(reader(userDirectoryPath+separator+list1.getSelectionModel().getSelectedItem()+separator+list2.getSelectionModel().getSelectedItem()),passwordKey));
    }
    @FXML
    private void add1() {
        final TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Создание группы");
        dialog.setHeaderText("Введите название новой группы");
        dialog.setContentText("Название группы:");
        final Optional<String> name = dialog.showAndWait();
        if (name.isPresent() && !isEmpty(name.get())) {
            File file = new File(userDirectoryPath+separator+name.get());
            if (file.exists()) {
                alertWindow("Группа с таким названием уже имеется\nВыберите другое название");
                add1();
                return;
            }
            file.mkdir();
            if (file.exists()) {
                showNotes(file.getParent());
                list1.getSelectionModel().select(file.getName());
                list2.setItems(null);
                text.clear();
            }else {
                alertWindow("Ошибка!\nВозможно вы ввели недопустимые символы");
                add1();
            }
        }
    }
    @FXML
    private void add2() throws IOException {
        String l=list1.getSelectionModel().getSelectedItem();
        if (l==null){
            alertWindow("Выберите группу");
            return;
        }
        final TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Создание записи");
        dialog.setHeaderText("Введите название новой записи");
        dialog.setContentText("Название записи:");
        final Optional<String> name = dialog.showAndWait();
        if (name.isPresent() && !isEmpty(name.get())) {
            if (incorrectSymbols(name.get())){
                alertWindow("Запись не будет создана:\nОбнаружены недопустимые символы в названии записи");
                add2();
                return;
            }
            File file = new File(userDirectoryPath+separator+l+separator+name.get());
            if (file.exists()) {
                alertWindow("Запись не будет создана:\nЗапись с таким названием уже имеется");
                add2();
                return;
            }
            if (file.createNewFile()){
                showNotes(file.getParent());
                list2.getSelectionModel().select(file.getName());
                text.clear();
            }
        }
    }
    @FXML
    private void del1(){
        String l=list1.getSelectionModel().getSelectedItem();
        if (l==null){
            alertWindow("Выберите группу");
            return;
        }
        File file=new File(userDirectoryPath+separator+l);
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтвердить");
        alert.setHeaderText("Удаление группы");
        alert.setContentText("Удалить группу " + file.getName() + "?");
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if (resultAlert.get() == ButtonType.OK) {
            deleteDirectory(file);
            if (!file.exists()) {
                showNotes(userDirectoryPath);
                list2.setItems(null);
                text.clear();
            }else {
                toast.setMessage("ошибка удаления");
            }
        }
    }
    @FXML
    private void del2(){
        String l1=list1.getSelectionModel().getSelectedItem();
        String l2=list2.getSelectionModel().getSelectedItem();
        if (l2==null){
            alertWindow("Выберите запись");
            return;
        }
        File file=new File(userDirectoryPath+separator+l1+separator+l2);
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Подтвердить");
        alert.setHeaderText("Удаление записи");
        alert.setContentText("Удалить запись " + file.getName() + "?");
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if (resultAlert.get() == ButtonType.OK) {
            if (file.delete()) {
                showNotes(file.getParent());
                text.clear();
            }else {
                toast.setMessage("ошибка удаления");
            }
        }
    }
    @FXML
    private void saveAction(){
        String l1=list1.getSelectionModel().getSelectedItem();
        String l2=list2.getSelectionModel().getSelectedItem();
        if (l1==null){
            alertWindow("Выберите или создайте группу");
            saveToClipboard();
            return;
        }
        if(l2==null){
            alertWindow("Выберите или создайте запись");
            saveToClipboard();
            return;
        }
        if(isEmpty(text.getText())){
            toast.setMessage("ничего нет для сохранения");
            return;
        }
        writer(userDirectoryPath + separator + l1+separator+l2 , cryptograph.encode(text.getText(),passwordKey));
        toast.setMessage("сохранение");
        }
    private void saveToClipboard(){
        if(!isEmpty(text.getText())){
            StringSelection ss = new StringSelection(text.getText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
            toast.setMessage("сохранено в буфер обмена");
        }
    }
    public void exit(){
        final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(500,140);
        alert.setTitle("ВЫХОД");
        alert.setHeaderText("Выйти из программы?");
        alert.setContentText("Несохраненные изменения будут потеряны навсегда.");
        final Optional<ButtonType> resultAlert = alert.showAndWait();
        if (resultAlert.get() == ButtonType.OK) {
            System.exit(0);
        }
    }
    private String reader(final String s) {
        StringBuilder f=new StringBuilder();
        try {
            final File file = new File(s);
            final BufferedReader br;
            try (FileReader fr = new FileReader(file)) {
                br = new BufferedReader(fr);
                String str;
                while ((str = br.readLine()) != null) {
                    f.append(str).append("\n");
                }
            }
            br.close();
        }
        catch (IOException e) {
            e.getMessage();
        }
        return f.toString();
    }
    private void writer(String pathFile,String text){
        try (final FileWriter fw = new FileWriter(pathFile)) {
            fw.write(text);
        } catch (IOException e) {
            e.getMessage();
        }
    }
    private boolean isEmpty(String s){
        return s == null || s.trim().length() == 0;
    }
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            assert children != null;
            for (String aChildren : children) {
                File f = new File(dir, aChildren);
                deleteDirectory(f);
            }
        }
        dir.delete();
    }
    private void showNotes(String p){
        File files=new File(p);
        File[] f=files.listFiles();
        assert f != null;
        String[] notes=new String[f.length];
        for(int i=0;i<notes.length;i++){
            notes[i]=f[i].getName();
        }
        if (p.equals(userDirectoryPath)) {
            list1.setItems(FXCollections.observableArrayList(notes).sorted());
        }else {
            list2.setItems(FXCollections.observableArrayList(notes).sorted());
        }
    }
    private void alertWindow(final String s) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setResizable(true);
        alert.getDialogPane().setPrefSize(400, 140);
        alert.setTitle("Внимание!");
        alert.setHeaderText("");
        alert.setContentText(s);
        alert.showAndWait();
    }
    private boolean incorrectSymbols(String str){
            return str.contains(":")||str.contains("*")||str.contains("/")||str.contains("|")
                    ||str.contains("?")||str.contains("#")||str.contains("!")||str.contains("$")||str.contains("%")||str.contains(";");
    }
    private boolean permissionRead(File file){
        if(!file.canRead()){
            file.setReadable(true);
            return !file.canRead();
        }
        return false;
    }
    private boolean permissionWrite(File file){
        if(!file.canWrite()){
            file.setWritable(true);
            return !file.canWrite();
        }
        return false;
    }
    private void dirCreator(final String fPath) {
        final File file = new File(fPath);
        if (!file.exists()) {
            file.mkdir();
            if(!file.exists()){
                alertWindow("Ошибка!\nКаталог <GroupNoteBook> не будет создан.\n" +
                        "Попробуйте создать указанный каталог вручную по следующему пути:\n"+fPath+"\nПрограмма будет закрыта.");
                System.exit(0); 
            }
        }
    }
    @Override
    public void initialize(URL location, ResourceBundle resources){
        String key = "satKLwrtZ157";
        String parentPath=null;
        try {
            parentPath= URLDecoder.decode(new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.getMessage();
        }
        String path=parentPath+separator+"GroupNoteBook";
        this.dirCreator(path);
        File f=new File(path);
        if(permissionRead(f)||permissionWrite(f)){
            if(permissionRead(f)&&permissionWrite(f)){
                alertWindow("Не удалось получить разрешение на чтение и запись файлов в каталог <GroupNoteBook>.\nПопробуйте дать разрешение вручную.");
            }else if(permissionRead(f)){
                alertWindow("Не удалось получить разрешение на чтение файлов в каталоге <GroupNoteBook>.\nПопробуйте дать разрешение вручную.");
            }else{
                alertWindow("Не удалось получить разрешение на запись файлов в каталог <GroupNoteBook>.\nПопробуйте дать разрешение вручную.");
            }
            System.exit(0);
        }
        Dialog dialog = new Dialog<>();
        dialog.setTitle("Вход в приложение");
        dialog.setHeaderText("Введите логин и пароль");

        ButtonType loginButtonType = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType registrationButtonType = new ButtonType("Регистрация",ButtonBar.ButtonData.FINISH);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType,cancelButtonType,registrationButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField login = new TextField();
        TextField password = new PasswordField();
       
        grid.add(new Label("Логин:"), 0, 0);
        grid.add(login, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if(result.get()==registrationButtonType){
            Dialog d = new Dialog<>();
            d.setTitle("Регистрация");
            d.setHeaderText("Придумайте логин и пароль");

            ButtonType doneButtonType = new ButtonType("Готово", ButtonBar.ButtonData.OK_DONE);
            ButtonType cButtonType  = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            d.getDialogPane().getButtonTypes().addAll(doneButtonType,cButtonType);

            GridPane g = new GridPane();
            g.setHgap(10);
            g.setVgap(10);
            g.setPadding(new Insets(20, 150, 10, 10));

            TextField loginField = new TextField();
            TextField passwordField = new TextField();
       
            g.add(new Label("Логин:"), 0, 0);
            g.add(loginField, 1, 0);
            g.add(new Label("Пароль:"), 0, 1);
            g.add(passwordField, 1, 1);

            d.getDialogPane().setContent(g);

            Optional<ButtonType> resulOptional = d.showAndWait();
            
        if(resulOptional.get()==doneButtonType){
            
            String loginString=loginField.getText().trim();
            String passwordString=passwordField.getText().trim();
            
            if (isEmpty(loginString)){
                alertWindow("Введите логин");
                initialize(location, resources);
            }
            if (isEmpty(passwordString)){
                alertWindow("Введите пароль");
                initialize(location, resources);
            }
            File file = new File(path+separator+loginString+separator+"Notes");
            if(file.exists()){
                alertWindow("Пользователь с логином "+loginString+" уже существует");
                initialize(location, resources);
            }else{
                if(file.mkdirs()){
                    writer(path+separator+loginString+separator+"p", cryptograph.encode(passwordString, key));
                    initialize(location, resources);
                }
            }
        }else{
            initialize(location, resources);
        }
        }
        if (result.get()==loginButtonType){
            
            String lString=login.getText().trim();
            String pString=password.getText().trim();
            
            if (isEmpty(lString)){
                alertWindow("Введите логин");
                initialize(location, resources);
            }
            if (isEmpty(pString)){
                alertWindow("Введите пароль");
                initialize(location, resources);
            }
            File file = new File(path+separator+lString);
            if(file.exists()){
                passwordKey = cryptograph.decode(reader(path+separator+lString+separator+"p"), key);
                if(pString.equals(passwordKey)){
                    userDirectoryPath=path+separator+lString+separator+"Notes";
                    userLogin.setText(lString);
                    showNotes(userDirectoryPath);
                }else{
                    alertWindow("Неверный пароль");
                    initialize(location, resources);
                }
            }else{
                alertWindow("Пользователь с логином "+lString+" не существует");
                initialize(location, resources);
            }
        }
        if(result.get()==cancelButtonType){
            System.exit(0);
        }
    }
}
