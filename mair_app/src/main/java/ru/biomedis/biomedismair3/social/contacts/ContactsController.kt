package ru.biomedis.biomedismair3.social.contacts

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.SortedList
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TabPane
import javafx.stage.Modality
import javafx.stage.WindowEvent
import ru.biomedis.biomedismair3.BaseController
import ru.biomedis.biomedismair3.BlockingAction
import ru.biomedis.biomedismair3.social.contacts.lenta.LentaController
import ru.biomedis.biomedismair3.social.contacts.lenta.UserLentaController
import ru.biomedis.biomedismair3.social.remote_client.SocialClient
import ru.biomedis.biomedismair3.social.remote_client.dto.ContactDto
import ru.biomedis.biomedismair3.social.remote_client.dto.error.ApiError
import ru.biomedis.biomedismair3.utils.Other.LoggerDelegate
import ru.biomedis.biomedismair3.utils.Other.Result
import ru.biomedis.biomedismair3.utils.TabHolder
import java.net.URL
import java.util.*

class ContactsController : BaseController(), TabHolder.Selected, TabHolder.Detached {

    private val log by LoggerDelegate()

    @FXML
    private lateinit var chatTabPane: TabPane

    @FXML
    private lateinit var contactsList: ListView<UserContact>

    @FXML
    private lateinit var followersCount: Label

    @FXML
    private lateinit var contactsCount: Label


    private val countContacts: SimpleIntegerProperty = SimpleIntegerProperty(0)

    private val contacts: ObservableList<UserContact> = FXCollections.observableArrayList()

    private val sortedContacts: SortedList<UserContact> = SortedList(contacts) { o1, o2 ->
        o2.contact.created.compareTo(o1.contact.created)
    }

    private var isContactsLoaded = false

    private lateinit var tabHolder: TabHolder

    override fun onCompletedInitialization() {
        tabHolder = TabHolder(controllerWindow, chatTabPane)
    }

    override fun onClose(event: WindowEvent) {

    }

    override fun setParams(vararg params: Any) {

    }

    override fun initialize(location: URL, resources: ResourceBundle) {

        contactsList.cellFactory = ContactUserCellFactory(
                this::getAbout,
                this::follow,
                this::deleteContact,
                this::showUserLenta
        )
        contactsList.items = sortedContacts

        contactsCount.textProperty().bind(countContacts.asString())
        countContacts.set(contacts.size)


        contactsList.setOnMouseClicked {
            if (it.clickCount == 2 ) {

                openChat(contactsList.selectionModel.selectedItem)

            }
        }
    }


    private fun openChat(contact: UserContact) {
        val chat = tabHolder.tabByFxId(contact.contact.id.toString())
        if (chat == null) tabHolder.addTab("/fxml/social/Chat.fxml", contact.account.login, true, contact.contact.id.toString(), contact.contact.contact)
        else chatTabPane.selectionModel.select(chat)
    }


    private fun showUserLenta(userid: Long) {
        UserLentaController.showLentaDialog(controllerWindow, userid)
    }


    private fun getAbout(userId: Long): String {
        val result = BlockingAction.actionResult(controllerWindow) {
            SocialClient.INSTANCE.accountClient.getAbout(userId)
        }
        if (result.isError) {
            showWarningDialog(
                    "Загрузка данных о пользователе",
                    "Загрузка данных не удалась",
                    "Перезапустите программу или попробуйте позже",
                    controllerWindow,
                    Modality.WINDOW_MODAL
            )
            log.error("", result.error)

        }
        return if (!result.isError) result.value else ""
    }

    private fun follow(contact: Long, follow: Boolean): Boolean {
        val result = BlockingAction.actionNoResult(controllerWindow) {
            if (follow) SocialClient.INSTANCE.contactsClient.follow(contact)
            else SocialClient.INSTANCE.contactsClient.unFollow(contact)
        }
        if (result.isError) {
            showWarningDialog(
                    "Подписка",
                    "Подписка не удалась",
                    "Перезапустите программу или попробуйте позже",
                    controllerWindow,
                    Modality.WINDOW_MODAL
            )
            log.error("", result.error)

        }
        return !result.isError
    }

    private fun deleteContact(userContact: UserContact) {
        val dialogResult = showConfirmationDialog(
                "Удаление контакта",
                "Контакт: ${userContact.account.login} будет удален.",
                "Вы уверены?",
                controllerWindow,
                Modality.WINDOW_MODAL

        )
        if (dialogResult.isPresent) {
            if (dialogResult.get() != okButtonType) return
        } else return

        val result = BlockingAction.actionNoResult(controllerWindow) {
            SocialClient.INSTANCE.contactsClient.deleteContact(userContact.contact.id)
        }
        if (result.isError) {
            showWarningDialog(
                    "Удаление контакта",
                    "Удаление не удалась",
                    "Перезапустите программу или попробуйте позже",
                    controllerWindow,
                    Modality.WINDOW_MODAL
            )
            log.error("", result.error)

        } else {
            contacts.remove(userContact)
            countContacts.set(contacts.size)
        }
    }

    /**
     * Открывает диалог поиска пользователей, добавляет выбранного пользователя
     */
    fun findUsers() {
        val usersToContacts: List<AccountSmallView> = FindUsersController.showFindUserDialog(controllerWindow)
        addContacts(usersToContacts)

    }

    private fun addContacts(usersToContacts: List<AccountSmallView>) {
        if (usersToContacts.isEmpty()) return
        val result: Result<List<ContactDto>> = BlockingAction.actionResult(controllerWindow) {
            SocialClient.INSTANCE.contactsClient.addAllContact(usersToContacts.map { it.id })
        }

        if (result.isError) {
            log.error("", result.error)
            if (result.error is ApiError) {
                val err = result.error as ApiError
                if (err.statusCode == 404) {
                    showErrorDialog(
                            "Добавление контактов",
                            "Ошибка добавления контактов",
                            "Один или более добавляемых контактов не найдены",
                            controllerWindow,
                            Modality.WINDOW_MODAL
                    )
                }
            } else showErrorDialog(
                    "Добавление контактов",
                    "Ошибка добавления контактов",
                    "",
                    controllerWindow,
                    Modality.WINDOW_MODAL
            )
            return
        }
        val contactsMap = result.value.groupBy { it.contact }
        //учитываем, что не все контакты могут добавиться - дублирование исключается, и список может вернуться пустым итп, отфильтруем не добавленные

        usersToContacts
                .filter { contactsMap.containsKey(it.id) }
                .map {
                    val contact = contactsMap[it.id]?.get(0)
                            ?: throw RuntimeException("Не верное сопоставление id")
                    UserContact(it, contact)
                }.let {
                    contacts.addAll(it)
                }

        countContacts.set(contacts.size)
    }

    /**
     * открывает диалог поиска по логину, добавляет найденного пользователя
     */
    fun addContact() {
        val user: AccountSmallView = AddUserController.showAddUserDialog(controllerWindow) ?: return
        addContacts(listOf(user))
    }

    private data class Contacts(val contacts: List<UserContact>, val countFollowers: Int)

    private fun loadContacts(): Contacts? {
        val result: Result<Contacts> = BlockingAction.actionResult(controllerWindow) {
            Contacts(
                    SocialClient.INSTANCE.contactsClient.allContacts(),
                    SocialClient.INSTANCE.contactsClient.followersCount()
            )

        }
        if (result.isError) {
            log.error("", result.error)
            showErrorDialog(
                    "Загрузка контактов",
                    "Ошибка загрузки контактов",
                    "Перезапустите программу и попробуйте позже",
                    controllerWindow,
                    Modality.WINDOW_MODAL
            )
            return null
        }

        return result.value
    }

    fun showLenta() {
        LentaController.showLentaDialog(controllerWindow)
    }


    override fun onSelected() {
        if (!isContactsLoaded) {
            val c = loadContacts()
            if (c != null) {
                contacts.addAll(c.contacts)
                followersCount.text = c.countFollowers.toString()
                countContacts.set(contacts.size)
            }
            isContactsLoaded = true
        }
    }

    override fun onDetach() {

    }


}
