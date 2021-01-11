package ru.biomedis.biomedismair3.social.registry

import javafx.collections.FXCollections
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.stage.Modality
import javafx.stage.WindowEvent
import ru.biomedis.biomedismair3.BaseController
import ru.biomedis.biomedismair3.BlockingAction
import ru.biomedis.biomedismair3.social.remote_client.SocialClient
import ru.biomedis.biomedismair3.social.remote_client.dto.DirectoryData
import ru.biomedis.biomedismair3.social.remote_client.dto.FileData
import ru.biomedis.biomedismair3.social.remote_client.dto.IFileItem
import ru.biomedis.biomedismair3.utils.Other.LoggerDelegate
import ru.biomedis.biomedismair3.utils.Other.Result
import ru.biomedis.biomedismair3.utils.TabHolder
import java.net.URL
import java.util.*
import kotlin.Comparator

class FilesController : BaseController(), TabHolder.Selected, TabHolder.Detached {

    private val log by LoggerDelegate()
    @FXML
    lateinit var pathLine: HBox
    @FXML
    lateinit var container: ListView<IFileItem>
    @FXML
    lateinit var newDirBtn: Button
    @FXML
    lateinit var newFileBtn: Button

    private var currentDirectory: DirectoryData? = null

    private val items = FXCollections.observableArrayList<IFileItem>()
    private val sortedItems = SortedList(items)

    override fun onCompletedInitialization() {

    }

    override fun onClose(event: WindowEvent) {

    }

    override fun setParams(vararg params: Any) {

    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        sortedItems.comparator = Comparator.comparing(IFileItem::directoryMarker)
            .thenComparing(Comparator.comparing(IFileItem::name))

        container.items = sortedItems
        container.cellFactory = FileCellFactory()


    }

    override fun onSelected() {
        loadRootDirectory()
    }

    override fun onDetach() {

    }


    private fun fillContainer(directories: List<DirectoryData>, files: List<FileData>) {
        items.addAll(directories)
        items.addAll(files)
    }

    private fun loadRootDirectory() {

        val result: Result<Pair<List<DirectoryData>, List<FileData>>> =
            BlockingAction.actionResult(controllerWindow) {
                SocialClient.INSTANCE.filesClient.getRootDirectories() to
                        SocialClient.INSTANCE.filesClient.getRootFiles()
            }

        if (result.isError) {
            log.error("", result.error)
            showWarningDialog(
                "Загрузка информации о файлах",
                "",
                "Загрузка не удалась",
                controllerWindow,
                Modality.WINDOW_MODAL
            )
        } else fillContainer(result.value.first, result.value.second)
    }

    @FXML
    private fun onNewDir() {
        val textInput: String = showTextInputDialog(
            "Создание папки",
            "Введите имя папки",
            "",
            "",
            controllerWindow,
            Modality.WINDOW_MODAL
        ).trim()

        if (textInput.isBlank()) {
            showWarningDialog(
                "Создание папки",
                "",
                "Имя папки не должно быть пустым",
                controllerWindow,
                Modality.WINDOW_MODAL
            )
            return
        }

        if (textInput.length > 255) {
            showWarningDialog(
                "Создание папки",
                "",
                "Имя папки не должно быть длиннее 255 символов",
                controllerWindow,
                Modality.WINDOW_MODAL
            )
            return
        }

        val result = BlockingAction.actionResult(controllerWindow) {
            if (currentDirectory == null) SocialClient.INSTANCE.filesClient.createInRootDirectory(
                textInput.trim()
            )
            else SocialClient.INSTANCE.filesClient.createDirectory(
                textInput.trim(),
                currentDirectory!!.id
            )
        }

        if (result.isError) {
            log.error("", result.error)
            showWarningDialog(
                "Создание новой папки",
                "",
                "Не удалось создать папку",
                controllerWindow,
                Modality.WINDOW_MODAL
            )
            return
        }

        addDirectoryToContainer(result.value)

    }

    private fun addDirectoryToContainer(dir: DirectoryData) {
       items.add(dir)
    }

    @FXML
    private fun onNewFile() {

    }

    @FXML
    private fun onSync() {
        //стирает весь кэш, начинает с корневой директории, те как в положении инициализации.
        //служит для исправления ошибок, если паралльно, что-то меняли в другой программе

    }

    //todo: Хлебные крошки програмно, не через сервер
}
