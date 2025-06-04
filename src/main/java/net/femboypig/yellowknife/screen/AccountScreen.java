package net.femboypig.yellowknife.screen;

import net.femboypig.yellowknife.client.YellowknifeClient;
import net.femboypig.yellowknife.util.AccountManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

import java.util.List;

public class AccountScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget usernameField;
    private AccountList accountList;
    private ButtonWidget addButton;
    private ButtonWidget switchButton;
    private ButtonWidget deleteButton;
    private ButtonWidget doneButton;
    
    // Status message
    private Text statusMessage = null;
    private long statusMessageTime = 0;
    private int statusMessageType = 0; // 0 - неважное, 1 - важное, 2 - ошибка/удаление

    // UI constants
    private static final int MAIN_PANEL_WIDTH = 320;
    private static final int MAIN_PANEL_HEIGHT = 240;
    private static final int BUTTON_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 30;
    private static final int BOTTOM_PANEL_HEIGHT = 60; // Высота нижней панели с кнопками
    private static final int BOTTOM_MARGIN = 70; // Отступ от низа, чтобы список не перекрывал кнопки
    private static final int ENTRY_HEIGHT = 28; // Ещё меньшая высота для элементов списка
    
    // UI colors
    private static final int COLOR_PANEL = 0x90101010;
    private static final int COLOR_PANEL_DARK = 0x90101020;
    private static final int COLOR_ACCENT = 0xFFFF69B4; // Розовый цвет
    private static final int COLOR_ACCENT_DARK = 0xFFFF1493; // Темно-розовый
    private static final int COLOR_TEXT = 0xFFEEEEEE;
    private static final int COLOR_TEXT_DARK = 0xFF999999;

    public AccountScreen(Screen parent) {
        super(new LiteralText(""));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int centerY = height / 2;
        
        int panelLeft = centerX - MAIN_PANEL_WIDTH / 2;
        int panelTop = centerY - MAIN_PANEL_HEIGHT / 2;
        int panelRight = centerX + MAIN_PANEL_WIDTH / 2;
        int panelBottom = centerY + MAIN_PANEL_HEIGHT / 2;
        
        // Определяем размеры кнопок и поля ввода
        int switchWidth = 100; // Ширина кнопки Switch Account
        int inputWidth = MAIN_PANEL_WIDTH - 140; // Ширина поля ввода
        
        // Простой вертикальный список аккаунтов с отступом снизу
        accountList = new AccountList(
                client,
                panelLeft + 10,
                panelTop + HEADER_HEIGHT + 5,
                MAIN_PANEL_WIDTH - 20,
                MAIN_PANEL_HEIGHT - HEADER_HEIGHT - BOTTOM_MARGIN
        );
        
        // Username field
        usernameField = new TextFieldWidget(
                textRenderer,
                panelLeft + 10,
                panelBottom - 50,
                inputWidth,
                20,
                new TranslatableText("yellowknife.account.username")
        );
        usernameField.setMaxLength(16);
        this.children.add(usernameField);

        // Add button
        addButton = new ButtonWidget(
            panelRight - 120, 
            panelBottom - 50, 
            110, 
            20,
            new TranslatableText("yellowknife.account.add"), 
            button -> {
                String username = usernameField.getText().trim();
                if (!username.isEmpty()) {
                    AccountManager.getInstance().addAccount(username);
                    usernameField.setText("");
                    refreshAccounts();
                    showStatusMessage(new TranslatableText("yellowknife.account.added", username), 1);
                }
            }
        );
        this.addButton(addButton);

        // Buttons for account operations
        switchButton = new ButtonWidget(
            panelLeft + 10, 
            panelBottom - 25, 
            switchWidth, 
            BUTTON_HEIGHT,
            new TranslatableText("yellowknife.account.switch"), 
            button -> {
                AccountManager.Account selected = accountList.getSelectedAccount();
                if (selected != null) {
                    if (AccountManager.getInstance().switchToAccount(selected)) {
                        showStatusMessage(new TranslatableText("yellowknife.account.switched", selected.getUsername()), 1);
                    }
                }
            }
        );
        this.addButton(switchButton);

        // Расширяем ширину кнопки Delete по формуле: ширина поля ввода - ширина Switch Account
        int deleteWidth = inputWidth - switchWidth;
        deleteButton = new ButtonWidget(
            panelLeft + 10 + switchWidth + 5, 
            panelBottom - 25, 
            deleteWidth - 5, 
            BUTTON_HEIGHT,
            new TranslatableText("yellowknife.account.delete"), 
            button -> {
                AccountManager.Account selected = accountList.getSelectedAccount();
                if (selected != null) {
                    AccountManager.getInstance().removeAccount(selected);
                    refreshAccounts();
                    showStatusMessage(new TranslatableText("yellowknife.account.deleted", selected.getUsername()), 2);
                }
            }
        );
        this.addButton(deleteButton);

        // Done button
        doneButton = new ButtonWidget(
            panelRight - 120, 
            panelBottom - 25, 
            110, 
            BUTTON_HEIGHT,
            new TranslatableText("gui.done"), 
            button -> close()
        );
        this.addButton(doneButton);

        refreshAccounts();
    }
    
    private void refreshAccounts() {
        AccountManager.getInstance().loadAccounts();
        accountList.setAccounts(AccountManager.getInstance().getAccounts());
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = accountList != null && accountList.getSelectedAccount() != null;
        switchButton.active = hasSelection;
        deleteButton.active = hasSelection;
    }

    private void showStatusMessage(Text message, int type) {
        statusMessage = message;
        statusMessageTime = System.currentTimeMillis();
        statusMessageType = type;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (accountList != null && accountList.mouseClicked(mouseX, mouseY, button)) {
            updateButtonStates();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (accountList != null) {
            return accountList.mouseScrolled(mouseX, mouseY, amount);
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.fillGradient(matrices, 0, 0, this.width, this.height, 0xFF000000, 0xFF000000);
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        int panelLeft = centerX - MAIN_PANEL_WIDTH / 2;
        int panelTop = centerY - MAIN_PANEL_HEIGHT / 2;
        int panelRight = centerX + MAIN_PANEL_WIDTH / 2;
        int panelBottom = centerY + MAIN_PANEL_HEIGHT / 2;
        
        // Draw panel
        fill(matrices, panelLeft, panelTop, panelRight, panelBottom, COLOR_PANEL);
        
        // Draw header
        fill(matrices, panelLeft, panelTop, panelRight, panelTop + HEADER_HEIGHT, COLOR_PANEL_DARK);
        fill(matrices, panelLeft, panelTop + HEADER_HEIGHT - 1, panelRight, panelTop + HEADER_HEIGHT, COLOR_ACCENT);
        
        // Дополнительно рисуем разделитель над нижней панелью, чтобы визуально отделить список
        int separatorY = panelBottom - BOTTOM_PANEL_HEIGHT;
        fill(matrices, panelLeft, separatorY, panelRight, separatorY + 1, COLOR_ACCENT_DARK);
        
        // Title с форматированием Current Account: имя_аккаунта
        String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
        LiteralText titlePrefix = new LiteralText("Current Account: ");
        titlePrefix.formatted(Formatting.WHITE);
        
        LiteralText userName = new LiteralText(currentUsername);
        userName.formatted(Formatting.GREEN);
        
        Text titleText = titlePrefix.copy().append(userName);
        
        drawCenteredText(matrices, textRenderer, titleText, centerX, panelTop + 10, 0xFFFFFF);
        
        // In 1.16.5, we need to manually set up scissor
        double scale = this.client.getWindow().getScaleFactor();
        int scissorX = (int) (scale * (panelLeft + 10));
        int scissorY = (int) (scale * (panelTop + HEADER_HEIGHT + 5));
        int scissorWidth = (int) (scale * (MAIN_PANEL_WIDTH - 20));
        int scissorHeight = (int) (scale * (MAIN_PANEL_HEIGHT - HEADER_HEIGHT - BOTTOM_MARGIN));
        
        // Use GL to set up scissor
        net.minecraft.client.util.Window window = this.client.getWindow();
        int frameHeight = window.getFramebufferHeight();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor(scissorX, frameHeight - scissorY - scissorHeight, scissorWidth, scissorHeight);
        
        // Render account list
        accountList.render(matrices, mouseX, mouseY, delta);
        
        // Disable scissor
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        
        // Render text field
        usernameField.render(matrices, mouseX, mouseY, delta);
        
        // Render the rest of UI widgets (buttons)
        super.render(matrices, mouseX, mouseY, delta);
        
        // Render status message at the bottom of the panel
        if (statusMessage != null && System.currentTimeMillis() - statusMessageTime < 3000) {
            Text formattedMessage;
            switch (statusMessageType) {
                case 1:
                    formattedMessage = statusMessage.copy().formatted(Formatting.GREEN);
                    break;
                case 2:
                    formattedMessage = statusMessage.copy().formatted(Formatting.DARK_RED);
                    break;
                default:
                    formattedMessage = statusMessage.copy().formatted(Formatting.WHITE);
                    break;
            }
            
            // Рисуем фон для уведомления
            int messageWidth = textRenderer.getWidth(formattedMessage.getString());
            int messageX = centerX - messageWidth / 2 - 5;
            int messageY = panelBottom - 78;
            fill(matrices, messageX, messageY, messageX + messageWidth + 10, messageY + 16, 0x80000000);
            fill(matrices, messageX, messageY, messageX + 2, messageY + 16, 
                statusMessageType == 2 ? 0xFFFF0000 : (statusMessageType == 1 ? 0xFF00FF00 : 0xFF444444));
            
            // Рисуем текст уведомления
            drawStringWithShadow(matrices, textRenderer, formattedMessage.getString(), messageX + 5, messageY + 4, 0xFFFFFF);
        }
    }

    public void close() {
        this.client.openScreen(this.parent);
    }

    private class AccountList {
        private final MinecraftClient client;
        private final int x, y, width, height;
        private List<AccountManager.Account> accounts;
        private int selectedIndex = -1;
        private int scrollOffset = 0;
        private int maxScrollOffset = 0;
        private int hoveredIndex = -1;
        
        private static final int ENTRY_HEIGHT = 28; // Ещё меньшая высота для элементов списка
        private static final int MAX_ENTRIES_VISIBLE = 6; // Увеличиваем количество видимых элементов
        
        public AccountList(MinecraftClient client, int x, int y, int width, int height) {
            this.client = client;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public void setAccounts(List<AccountManager.Account> accounts) {
            this.accounts = accounts;
            if (accounts.isEmpty()) {
                selectedIndex = -1;
            } else {
                maxScrollOffset = Math.max(0, accounts.size() * ENTRY_HEIGHT - height);
                
                if (selectedIndex >= accounts.size() || selectedIndex < 0) {
                    selectedIndex = accounts.isEmpty() ? -1 : 0;
                }
                
                ensureSelectedVisible();
            }
        }
        
        public AccountManager.Account getSelectedAccount() {
            if (selectedIndex >= 0 && selectedIndex < accounts.size()) {
                return accounts.get(selectedIndex);
            }
            return null;
        }
        
        private void ensureSelectedVisible() {
            if (selectedIndex >= 0) {
                int selectedTop = selectedIndex * ENTRY_HEIGHT;
                int selectedBottom = selectedTop + ENTRY_HEIGHT;
                
                if (selectedTop < scrollOffset) {
                    scrollOffset = selectedTop;
                }
                
                else if (selectedBottom > scrollOffset + height) {
                    scrollOffset = selectedBottom - height;
                }
                
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
            }
        }
        
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (accounts == null || accounts.isEmpty()) {
                // Улучшенное отображение "Нет аккаунтов"
                int centerX = x + width / 2;
                int centerY = y + height / 2;
                
                // Рисуем фоновую панель с градиентом
                fillGradient(
                    matrices,
                    x + 10, 
                    centerY - 20, 
                    x + width - 10, 
                    centerY + 20, 
                    0x40303030, 
                    0x60505050
                );
                
                // Акцентная рамка вокруг панели
                fill(matrices, x + 10, centerY - 20, x + width - 10, centerY - 19, COLOR_ACCENT_DARK);
                fill(matrices, x + 10, centerY + 19, x + width - 10, centerY + 20, COLOR_ACCENT_DARK);
                fill(matrices, x + 10, centerY - 20, x + 11, centerY + 20, COLOR_ACCENT_DARK);
                fill(matrices, x + width - 11, centerY - 20, x + width - 10, centerY + 20, COLOR_ACCENT_DARK);
                
                // Текст "Нет аккаунтов" с тенью и стильным форматированием
                TranslatableText noAccountsText = new TranslatableText("yellowknife.account.no_accounts");
                noAccountsText.formatted(Formatting.BOLD);
                drawCenteredText(matrices, textRenderer, noAccountsText, centerX, centerY - 10, COLOR_ACCENT);
                
                // Подсказка как добавить аккаунт
                TranslatableText helpText = new TranslatableText("yellowknife.account.add_hint");
                helpText.formatted(Formatting.GRAY);
                drawCenteredText(matrices, textRenderer, helpText, centerX, centerY + 5, COLOR_TEXT_DARK);
                return;
            }
            
            // Определяем, находится ли мышь над каким-либо элементом
            hoveredIndex = -1;
            if (mouseX >= x && mouseX < x + width) {
                for (int i = 0; i < accounts.size(); i++) {
                    int entryY = y + i * ENTRY_HEIGHT - scrollOffset;
                    if (entryY + ENTRY_HEIGHT > y && entryY < y + height) { // Only check if entry is visible
                        if (mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                            hoveredIndex = i;
                            break;
                        }
                    }
                }
            }
            
            // Нужен ли скроллбар
            boolean needsScrolling = accounts.size() * ENTRY_HEIGHT > height;
            
            // Render account entries
            for (int i = 0; i < accounts.size(); i++) {
                int entryY = y + i * ENTRY_HEIGHT - scrollOffset;
                
                // Skip rendering if entry is completely outside visible area
                if (entryY + ENTRY_HEIGHT <= y || entryY >= y + height) continue;
                
                AccountManager.Account account = accounts.get(i);
                boolean isSelected = i == selectedIndex;
                boolean isHovered = i == hoveredIndex;
                
                renderAccountEntry(matrices, account, x, entryY, width, ENTRY_HEIGHT, isSelected, isHovered, needsScrolling);
            }
            
            // Render scroll indicators if needed
            if (needsScrolling) {
                renderScrollIndicators(matrices);
            }
        }
        
        public void renderScrollIndicators(MatrixStack matrices) {
            // Полоса прокрутки (улучшенный вид)
            int scrollBarHeight = Math.max(30, height * height / (accounts.size() * ENTRY_HEIGHT));
            
            // Вычисляем позицию скроллбара с учетом максимального значения
            float scrollPercentage = (float) scrollOffset / maxScrollOffset;
            int scrollBarY = y + (int) ((height - scrollBarHeight) * scrollPercentage);
            
            // Фон полосы с розовым оттенком
            fill(matrices, x + width - 6, y, x + width, y + height, 0x20FF69B4);
            // Сама полоса с розовым оттенком (более широкая и заметная)
            fill(matrices, x + width - 6, scrollBarY, x + width, scrollBarY + scrollBarHeight, 0xAAFF69B4);
            
            // Добавляем небольшой блик на скроллбаре
            fill(matrices, x + width - 6, scrollBarY, x + width - 4, scrollBarY + scrollBarHeight, 0xFFFF8CC5);
        }
        
        private void renderAccountEntry(MatrixStack matrices, AccountManager.Account account, int x, int y, int width, int height, 
                                      boolean isSelected, boolean isHovered, boolean needsScrolling) {
            // Добавляем отступы между элементами
            int verticalPadding = 2;
            int horizontalPadding = 10;
            
            // Отступ для скроллбара применяем только если он нужен
            int rightPadding = needsScrolling ? 10 : 0;
            int adjustedWidth = width - rightPadding;
            
            // Проверяем, является ли этот аккаунт текущим активным
            String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
            boolean isCurrentAccount = account.getUsername().equals(currentUsername);
            
            // Background с градиентом и отступами для визуального разделения элементов (стиль карточки)
            if (isSelected) {
                // Градиентный фон для выделенного элемента (от более яркого к более темному)
                fillGradient(
                    matrices,
                    x + horizontalPadding, 
                    y + verticalPadding, 
                    x + adjustedWidth - horizontalPadding, 
                    y + height - verticalPadding, 
                    0x90FF69B4, // Верхний цвет (более яркий)
                    0x80FF1493  // Нижний цвет (более темный)
                );
            } else if (isHovered) {
                // Эффект при наведении - более светлый фон с легким градиентом
                fillGradient(
                    matrices,
                    x + horizontalPadding, 
                    y + verticalPadding, 
                    x + adjustedWidth - horizontalPadding, 
                    y + height - verticalPadding, 
                    0x50505050, // Верхний цвет
                    0x60606060  // Нижний цвет
                );
            } else {
                // Обычный фон с небольшой прозрачностью и эффектом градиента
                fillGradient(
                    matrices,
                    x + horizontalPadding, 
                    y + verticalPadding, 
                    x + adjustedWidth - horizontalPadding, 
                    y + height - verticalPadding, 
                    0x30202030, // Верхний цвет
                    0x40202030  // Нижний цвет
                );
            }
            
            // Рамка вокруг элемента
            if (isSelected) {
                // Добавляем рамку вокруг выделенного элемента
                fill(matrices, x + horizontalPadding, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + verticalPadding + 1, COLOR_ACCENT); // Верхняя
                fill(matrices, x + horizontalPadding, y + height - verticalPadding - 1, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, COLOR_ACCENT); // Нижняя
                fill(matrices, x + horizontalPadding, y + verticalPadding, x + horizontalPadding + 1, y + height - verticalPadding, COLOR_ACCENT); // Левая
                fill(matrices, x + adjustedWidth - horizontalPadding - 1, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, COLOR_ACCENT); // Правая
            } else if (isHovered) {
                // Добавляем тонкую рамку при наведении
                int hoverBorderColor = 0x60FF69B4; // Полупрозрачный розовый
                fill(matrices, x + horizontalPadding, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + verticalPadding + 1, hoverBorderColor); // Верхняя
                fill(matrices, x + horizontalPadding, y + height - verticalPadding - 1, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, hoverBorderColor); // Нижняя
                fill(matrices, x + horizontalPadding, y + verticalPadding, x + horizontalPadding + 1, y + height - verticalPadding, hoverBorderColor); // Левая
                fill(matrices, x + adjustedWidth - horizontalPadding - 1, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, hoverBorderColor); // Правая
            }
            
            // Account name - центрируем по вертикали
            String username = account.getUsername();
            
            // Координаты для имени аккаунта
            int textX = x + horizontalPadding + 5;
            int nameY = y + (height - textRenderer.fontHeight) / 2; // Центрируем текст по вертикали
            
            // Определяем цвет текста в зависимости от состояния
            int textColor;
            if (isSelected) {
                textColor = 0xFFFFFFFF; // Белый для выделенного
            } else if (isCurrentAccount) {
                textColor = 0xFF00FF00; // Зеленый для текущего
            } else {
                textColor = COLOR_TEXT; // Обычный цвет для остальных
            }
            
            // Рисуем имя аккаунта с соответствующим цветом
            drawStringWithShadow(matrices, textRenderer, username, textX, nameY, textColor);
            
            // Информация о дате добавления - используем перевод "Added" как в оригинале
            String addedInfo = new TranslatableText("yellowknife.account.added_date", getFormattedDate(account)).getString();
            int dateWidth = textRenderer.getWidth(addedInfo);
            
            // Определяем положение для текста "In use"
            if (isCurrentAccount) {
                TranslatableText inUseText = new TranslatableText("yellowknife.account.in_use");
                inUseText.formatted(Formatting.GREEN);
                int usernameWidth = textRenderer.getWidth(username);
                int inUseWidth = textRenderer.getWidth(inUseText.getString());
                int padding = 5; // Отступ между текстами
                
                // Вычисляем доступное пространство
                int availableWidth = adjustedWidth - horizontalPadding * 2 - 10 - usernameWidth - dateWidth;
                
                // Расчет позиции для "In use" чтобы он был посередине между именем и датой
                int inUseX = textX + usernameWidth + padding;
                
                // Рисуем "In use"
                drawStringWithShadow(
                    matrices,
                    textRenderer,
                    inUseText.getString(),
                    inUseX,
                    nameY,
                    0xFF00FF00 // Зеленый цвет
                );
            }
            
            // Рисуем информацию о дате добавления всегда справа (с центрированием по Y)
            drawStringWithShadow(
                matrices, 
                textRenderer, 
                addedInfo, 
                x + adjustedWidth - horizontalPadding - dateWidth - 5,
                nameY, // Используем ту же Y координату для центрирования
                0xFFAAAAAA // Серый цвет для дополнительной информации
            );
        }
        
        // Метод для получения форматированной даты добавления
        private String getFormattedDate(AccountManager.Account account) {
            if (account == null) {
                return "unknown";
            }
            
            try {
                // Используем реальную дату из объекта аккаунта
                return account.getFormattedCreationDate();
            } catch (Exception e) {
                // Если возникла ошибка при получении даты, используем запасной вариант
                long hash = Math.abs(account.getUsername().hashCode());
                long day = hash % 30 + 1;
                long month = (hash / 30) % 12 + 1;
                long year = 2023 + (hash / 360) % 2;
                
                return String.format("%02d/%02d/%d", day, month, year);
            }
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (accounts == null || accounts.isEmpty() || button != 0) {
                return false;
            }
            
            // Клик на полосе прокрутки
            if (accounts.size() * ENTRY_HEIGHT > height) {
                if (mouseX >= x + width - 6 && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    int clickPosition = (int)(mouseY - y);
                    scrollOffset = (int)(clickPosition * maxScrollOffset / height);
                    scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
                    return true;
                }
            }
            
            // Клик на элементе списка
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                int relativeY = (int) (mouseY - y + scrollOffset);
                int clickedIndex = relativeY / ENTRY_HEIGHT;
                
                if (clickedIndex >= 0 && clickedIndex < accounts.size()) {
                    // Двойной клик для переключения на аккаунт
                    if (clickedIndex == selectedIndex) {
                        long now = System.currentTimeMillis();
                        if (now - lastClickTime < 500 && lastClickedIndex == clickedIndex) {
                            // Двойной клик обнаружен - переключаемся на этот аккаунт
                            AccountManager.Account account = accounts.get(clickedIndex);
                            AccountManager.getInstance().switchToAccount(account);
                            lastClickTime = 0; // Сбрасываем время клика
                            return true;
                        }
                    }
                    
                    // Обычный клик - выбираем аккаунт
                    selectedIndex = clickedIndex;
                    lastClickedIndex = clickedIndex;
                    lastClickTime = System.currentTimeMillis();
                    ensureSelectedVisible();
                    return true;
                }
            }
            
            return false;
        }
        
        private int lastClickedIndex = -1;
        private long lastClickTime = 0;
        
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            if (accounts == null || accounts.isEmpty() || maxScrollOffset <= 0) {
                return false;
            }
            
            // Корректируем скорость прокрутки
            double scrollSpeed = 20.0; // Увеличиваем скорость прокрутки
            int scrollAmount = (int) (-amount * scrollSpeed);
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + scrollAmount));
            
            return true;
        }
    }
} 