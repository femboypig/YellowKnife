package net.femboypig.yellowknife.screen;

import net.femboypig.yellowknife.client.YellowknifeClient;
import net.femboypig.yellowknife.util.AccountManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
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
        super(Text.literal(""));
        this.parent = parent;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Переопределяем метод, чтобы не рендерить размытие и нечеткости
        // Не вызываем super.renderBackground()
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
                Text.translatable("yellowknife.account.username")
        );
        usernameField.setMaxLength(16);
        usernameField.setPlaceholder(Text.translatable("yellowknife.account.username_placeholder"));
        addDrawableChild(usernameField);

        // Add button
        addButton = ButtonWidget.builder(Text.translatable("yellowknife.account.add"), button -> {
            String username = usernameField.getText().trim();
            if (!username.isEmpty()) {
                AccountManager.getInstance().addAccount(username);
                usernameField.setText("");
                refreshAccounts();
                showStatusMessage(Text.translatable("yellowknife.account.added", username), 1);
            }
        })
        .dimensions(panelRight - 120, panelBottom - 50, 110, 20)
        .build();
        addDrawableChild(addButton);

        // Buttons for account operations
        switchButton = ButtonWidget.builder(Text.translatable("yellowknife.account.switch"), button -> {
            AccountManager.Account selected = accountList.getSelectedAccount();
            if (selected != null) {
                if (AccountManager.getInstance().switchToAccount(selected)) {
                    showStatusMessage(Text.translatable("yellowknife.account.switched", selected.getUsername()), 1);
                }
            }
        })
        .dimensions(panelLeft + 10, panelBottom - 25, switchWidth, BUTTON_HEIGHT)
        .build();
        addDrawableChild(switchButton);

        // Расширяем ширину кнопки Delete по формуле: ширина поля ввода - ширина Switch Account
        int deleteWidth = inputWidth - switchWidth;
        deleteButton = ButtonWidget.builder(Text.translatable("yellowknife.account.delete"), button -> {
            AccountManager.Account selected = accountList.getSelectedAccount();
            if (selected != null) {
                AccountManager.getInstance().removeAccount(selected);
                refreshAccounts();
                showStatusMessage(Text.translatable("yellowknife.account.deleted", selected.getUsername()), 2);
            }
        })
        .dimensions(panelLeft + 10 + switchWidth + 5, panelBottom - 25, deleteWidth - 5, BUTTON_HEIGHT)
        .build();
        addDrawableChild(deleteButton);

        // Done button
        doneButton = ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(panelRight - 120, panelBottom - 25, 110, BUTTON_HEIGHT)
                .build();
        addDrawableChild(doneButton);

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
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Отладочный вывод
        System.out.println("Scroll event detected: " + verticalAmount);
        
        // Проверяем, что список не пустой
        if (accountList != null) {
            // Прокручиваем список, предавая только вертикальную составляющую прокрутки
            return accountList.mouseScrolled(mouseX, mouseY, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рендерим ЧИСТО черный фон вместо размытого
        context.fill(0, 0, this.width, this.height, 0xFF000000);
        
        int centerX = width / 2;
        int centerY = height / 2;
        
        int panelLeft = centerX - MAIN_PANEL_WIDTH / 2;
        int panelTop = centerY - MAIN_PANEL_HEIGHT / 2;
        int panelRight = centerX + MAIN_PANEL_WIDTH / 2;
        int panelBottom = centerY + MAIN_PANEL_HEIGHT / 2;
        
        // Draw panel
        context.fill(panelLeft, panelTop, panelRight, panelBottom, COLOR_PANEL);
        
        // Draw header
        context.fill(panelLeft, panelTop, panelRight, panelTop + HEADER_HEIGHT, COLOR_PANEL_DARK);
        context.fill(panelLeft, panelTop + HEADER_HEIGHT - 1, panelRight, panelTop + HEADER_HEIGHT, COLOR_ACCENT);
        
        // Дополнительно рисуем разделитель над нижней панелью, чтобы визуально отделить список
        int separatorY = panelBottom - BOTTOM_PANEL_HEIGHT;
        context.fill(panelLeft, separatorY, panelRight, separatorY + 1, COLOR_ACCENT_DARK);
        
        // Title с форматированием Current Account: имя_аккаунта
        String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
        Text titlePrefix = Text.literal("Current Account: ").formatted(Formatting.WHITE);
        Text userName = Text.literal(currentUsername).formatted(Formatting.GREEN);
        Text titleText = titlePrefix.copy().append(userName);
        
        context.drawCenteredTextWithShadow(textRenderer, titleText, centerX, panelTop + 10, 0xFFFFFF);
        
        // Используем scissor для обрезки списка, чтобы он не выходил за границы
        context.enableScissor(
            panelLeft + 10, 
            panelTop + HEADER_HEIGHT + 5,
            panelRight - 10,
            panelBottom - BOTTOM_PANEL_HEIGHT
        );
        
        // Render account list
        accountList.render(context, mouseX, mouseY, delta);
        
        context.disableScissor();
        
        // Render UI widgets
        super.render(context, mouseX, mouseY, delta);
        
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
            int messageWidth = textRenderer.getWidth(formattedMessage);
            int messageX = centerX - messageWidth / 2 - 5;
            int messageY = panelBottom - 78;
            int messageHeight = 16;
            
            // Полупрозрачный фон под уведомлением
            context.fill(messageX, messageY, messageX + messageWidth + 10, messageY + messageHeight, 0x80000000);
            
            // Рисуем сообщение
            context.drawCenteredTextWithShadow(textRenderer, formattedMessage, centerX, messageY + 4, 0xFFFFFF);
        } else {
            statusMessage = null;
        }
        
        // Явный рендеринг скролл-индикаторов, если они нужны
        if (accountList != null) {
            accountList.renderScrollIndicators(context);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    /**
     * Простой вертикальный список аккаунтов
     */
    private class AccountList {
        private final MinecraftClient client;
        private final int x, y, width, height;
        private List<AccountManager.Account> accounts;
        private int selectedIndex = -1;
        private int scrollOffset = 0;
        
        // Добавляем переменную для отслеживания элемента под мышью
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
            
            // Reset selection if needed
            if (accounts.isEmpty()) {
                selectedIndex = -1;
            } else if (selectedIndex >= accounts.size()) {
                selectedIndex = accounts.size() - 1;
            } else if (selectedIndex == -1 && !accounts.isEmpty()) {
                selectedIndex = 0;
            }
            
            // Reset scroll if needed
            if (selectedIndex >= 0) {
                ensureSelectedVisible();
            } else {
                scrollOffset = 0;
            }
        }
        
        public AccountManager.Account getSelectedAccount() {
            if (selectedIndex >= 0 && selectedIndex < accounts.size()) {
                return accounts.get(selectedIndex);
            }
            return null;
        }
        
        private void ensureSelectedVisible() {
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            } else if (selectedIndex >= scrollOffset + MAX_ENTRIES_VISIBLE) {
                scrollOffset = selectedIndex - MAX_ENTRIES_VISIBLE + 1;
            }
            
            // Исправляем расчет максимального смещения прокрутки
            // Используем более строгий расчет, чтобы последний элемент был виден полностью
            int maxScrollOffset = Math.max(0, accounts.size() - MAX_ENTRIES_VISIBLE);
            
            // Применяем отступ, если мы находимся у последнего элемента
            if (selectedIndex == accounts.size() - 1) {
                maxScrollOffset += 1; // Добавляем отступ для последнего элемента
            }
            
            scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
            if (accounts.size() <= MAX_ENTRIES_VISIBLE) {
                scrollOffset = 0;
            }
        }
        
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            if (accounts == null || accounts.isEmpty()) {
                // Улучшенное отображение "Нет аккаунтов"
                int centerX = x + width / 2;
                int centerY = y + height / 2;
                
                // Рисуем фоновую панель с градиентом
                context.fillGradient(
                    x + 10, 
                    centerY - 20, 
                    x + width - 10, 
                    centerY + 20, 
                    0x40303030, 
                    0x60505050
                );
                
                // Акцентная рамка вокруг панели
                context.fill(x + 10, centerY - 20, x + width - 10, centerY - 19, COLOR_ACCENT_DARK);
                context.fill(x + 10, centerY + 19, x + width - 10, centerY + 20, COLOR_ACCENT_DARK);
                context.fill(x + 10, centerY - 20, x + 11, centerY + 20, COLOR_ACCENT_DARK);
                context.fill(x + width - 11, centerY - 20, x + width - 10, centerY + 20, COLOR_ACCENT_DARK);
                
                // Текст "Нет аккаунтов" с тенью и стильным форматированием
                Text noAccountsText = Text.translatable("yellowknife.account.no_accounts").formatted(Formatting.BOLD);
                context.drawCenteredTextWithShadow(textRenderer, noAccountsText, centerX, centerY - 10, COLOR_ACCENT);
                
                // Подсказка как добавить аккаунт
                Text helpText = Text.translatable("yellowknife.account.add_hint").formatted(Formatting.GRAY);
                context.drawCenteredTextWithShadow(textRenderer, helpText, centerX, centerY + 5, COLOR_TEXT_DARK);
                return;
            }
            
            // Определяем, находится ли мышь над каким-либо элементом
            hoveredIndex = -1;
            if (mouseX >= x && mouseX < x + width - 10) {
                for (int i = 0; i < Math.min(MAX_ENTRIES_VISIBLE, accounts.size() - scrollOffset); i++) {
                    int entryY = y + i * ENTRY_HEIGHT;
                    if (mouseY >= entryY && mouseY < entryY + ENTRY_HEIGHT) {
                        hoveredIndex = i + scrollOffset;
                        break;
                    }
                }
            }
            
            // Draw accounts
            int startIndex = scrollOffset;
            int endIndex = Math.min(accounts.size(), startIndex + MAX_ENTRIES_VISIBLE);
            
            for (int i = startIndex; i < endIndex; i++) {
                AccountManager.Account account = accounts.get(i);
                boolean isSelected = i == selectedIndex;
                boolean isHovered = i == hoveredIndex;
                
                renderAccountEntry(context, account, x, y + (i - startIndex) * ENTRY_HEIGHT, width, ENTRY_HEIGHT, isSelected, isHovered);
            }
        }
        
        // Метод для явного рендеринга скролл-индикаторов
        public void renderScrollIndicators(DrawContext context) {
            if (accounts == null || accounts.size() <= MAX_ENTRIES_VISIBLE) {
                return;
            }
            
            // Полоса прокрутки (улучшенный вид)
            if (accounts.size() > 0) {
                int scrollBarHeight = Math.max(30, height * MAX_ENTRIES_VISIBLE / accounts.size());
                
                // Используем те же расчеты максимального скролла, что и везде
                // Учитываем дополнительный отступ, который используется в mouseScrolled
                int maxScrollOffset = Math.max(0, accounts.size() - MAX_ENTRIES_VISIBLE);
                if (scrollOffset >= maxScrollOffset) {
                    maxScrollOffset += 1; // Добавляем такой же отступ как в mouseScrolled
                }
                
                // Вычисляем позицию скроллбара с учетом максимального значения
                int scrollBarY = y;
                if (maxScrollOffset > 0) {
                    scrollBarY = y + (height - scrollBarHeight) * scrollOffset / maxScrollOffset;
                }
                
                // Фон полосы с розовым оттенком
                context.fill(x + width - 6, y, x + width, y + height, 0x20FF69B4);
                // Сама полоса с розовым оттенком (более широкая и заметная)
                context.fill(x + width - 6, scrollBarY, x + width, scrollBarY + scrollBarHeight, 0xAAFF69B4);
                
                // Добавляем небольшой блик на скроллбаре
                context.fill(x + width - 6, scrollBarY, x + width - 4, scrollBarY + scrollBarHeight, 0xFFFF8CC5);
            }
        }
        
        private void renderAccountEntry(DrawContext context, AccountManager.Account account, int x, int y, int width, int height, boolean isSelected, boolean isHovered) {
            // Добавляем отступы между элементами
            int verticalPadding = 2;
            int horizontalPadding = 10;
            
            // Проверяем, нужен ли скроллбар
            boolean needsScrolling = accounts.size() > MAX_ENTRIES_VISIBLE;
            // Отступ для скроллбара применяем только если он нужен
            int rightPadding = needsScrolling ? 10 : 0;
            int adjustedWidth = width - rightPadding;
            
            // Проверяем, является ли этот аккаунт текущим активным
            String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
            boolean isCurrentAccount = account.getUsername().equals(currentUsername);
            
            // Background с градиентом и отступами для визуального разделения элементов (стиль карточки)
            if (isSelected) {
                // Градиентный фон для выделенного элемента (от более яркого к более темному)
                context.fillGradient(
                    x + horizontalPadding, 
                    y + verticalPadding, 
                    x + adjustedWidth - horizontalPadding, 
                    y + height - verticalPadding, 
                    0x90FF69B4, // Верхний цвет (более яркий)
                    0x80FF1493  // Нижний цвет (более темный)
                );
            } else if (isHovered) {
                // Эффект при наведении - более светлый фон с легким градиентом
                context.fillGradient(
                    x + horizontalPadding, 
                    y + verticalPadding, 
                    x + adjustedWidth - horizontalPadding, 
                    y + height - verticalPadding, 
                    0x50505050, // Верхний цвет
                    0x60606060  // Нижний цвет
                );
            } else {
                // Обычный фон с небольшой прозрачностью и эффектом градиента
                context.fillGradient(
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
                context.fill(x + horizontalPadding, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + verticalPadding + 1, COLOR_ACCENT); // Верхняя
                context.fill(x + horizontalPadding, y + height - verticalPadding - 1, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, COLOR_ACCENT); // Нижняя
                context.fill(x + horizontalPadding, y + verticalPadding, x + horizontalPadding + 1, y + height - verticalPadding, COLOR_ACCENT); // Левая
                context.fill(x + adjustedWidth - horizontalPadding - 1, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, COLOR_ACCENT); // Правая
            } else if (isHovered) {
                // Добавляем тонкую рамку при наведении
                int hoverBorderColor = 0x60FF69B4; // Полупрозрачный розовый
                context.fill(x + horizontalPadding, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + verticalPadding + 1, hoverBorderColor); // Верхняя
                context.fill(x + horizontalPadding, y + height - verticalPadding - 1, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, hoverBorderColor); // Нижняя
                context.fill(x + horizontalPadding, y + verticalPadding, x + horizontalPadding + 1, y + height - verticalPadding, hoverBorderColor); // Левая
                context.fill(x + adjustedWidth - horizontalPadding - 1, y + verticalPadding, x + adjustedWidth - horizontalPadding, y + height - verticalPadding, hoverBorderColor); // Правая
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
            context.drawTextWithShadow(textRenderer, username, textX, nameY, textColor);
            
            // Информация о дате добавления
            String addedInfo = Text.translatable("yellowknife.account.added_date", getFormattedDate(account, true)).getString();
            int dateWidth = textRenderer.getWidth(addedInfo);
            
            // Определяем положение для текста "In use"
            if (isCurrentAccount) {
                Text inUseText = Text.translatable("yellowknife.account.in_use").formatted(Formatting.GREEN);
                int usernameWidth = textRenderer.getWidth(username);
                int inUseWidth = textRenderer.getWidth(inUseText);
                int padding = 5; // Отступ между текстами
                
                // Вычисляем доступное пространство
                int availableWidth = adjustedWidth - horizontalPadding * 2 - 10 - usernameWidth - dateWidth;
                
                // Расчет позиции для "In use" чтобы он был посередине между именем и датой
                int inUseX = textX + usernameWidth + padding;
                
                // Рисуем "In use"
                context.drawTextWithShadow(
                    textRenderer,
                    inUseText,
                    inUseX,
                    nameY,
                    0xFFFFFFFF // Белый цвет (форматирование GREEN уже применено)
                );
            }
            
            // Рисуем информацию о дате добавления всегда справа (с центрированием по Y)
            context.drawTextWithShadow(
                textRenderer, 
                addedInfo, 
                x + adjustedWidth - horizontalPadding - dateWidth - 5,
                nameY, // Используем ту же Y координату для центрирования
                0xFFAAAAAA // Серый цвет для дополнительной информации
            );
        }
        
        // Метод для получения форматированной даты добавления или последнего входа аккаунта
        private String getFormattedDate(AccountManager.Account account, boolean isCreationDate) {
            if (account == null) {
                return "unknown";
            }
            
            try {
                // Используем реальные даты из объекта аккаунта
                if (isCreationDate) {
                    // Дата создания аккаунта
                    return account.getFormattedCreationDate();
                } else {
                    // Дата последнего входа
                    return account.getFormattedLastLoginDate();
                }
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
            if (accounts.size() > MAX_ENTRIES_VISIBLE) {
                if (mouseX >= x + width - 6 && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    int clickPosition = (int)(mouseY - y);
                    // Обновляем логику расчета максимального scrollOffset
                    int maxScrollOffset = Math.max(0, accounts.size() - MAX_ENTRIES_VISIBLE);
                    
                    // Используем тот же подход с дополнительным отступом для последнего элемента
                    int lastPosition = height - 1; // Позиция клика для последнего элемента
                    if (clickPosition >= lastPosition * 0.95) { // Если клик близко к концу скроллбара
                        maxScrollOffset += 1; // Добавляем дополнительный отступ
                    }
                    
                    scrollOffset = clickPosition * maxScrollOffset / height;
                    scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset));
                    return true;
                }
            }
            
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                int entryIndex = (int)((mouseY - y) / ENTRY_HEIGHT) + scrollOffset;
                
                if (entryIndex >= 0 && entryIndex < accounts.size()) {
                    selectedIndex = entryIndex;
                    ensureSelectedVisible();
                    return true;
                }
            }
            
            return false;
        }
        
        public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
            // Отладочный вывод
            System.out.println("AccountList scroll: " + amount + ", accounts: " + (accounts != null ? accounts.size() : "null"));
            
            if (accounts == null || accounts.isEmpty()) {
                return false;
            }
            
            // Проверяем наличие скролла только если элементов больше чем может поместиться
            boolean needsScrolling = accounts.size() > MAX_ENTRIES_VISIBLE;
            System.out.println("Needs scrolling: " + needsScrolling);
            
            if (!needsScrolling) {
                return false;
            }
            
            // Корректируем скорость прокрутки
            double scrollSpeed = 1.0; // Уменьшаем скорость прокрутки для более плавного движения
            int newScrollOffset = scrollOffset - (int)(amount * scrollSpeed);
            
            // Проверяем, что новое значение прокрутки валидно
            // Добавляем нижний отступ в 1 элемент для полной видимости последнего элемента
            int maxScrollOffset = Math.max(0, accounts.size() - MAX_ENTRIES_VISIBLE);
            
            // Если мы находимся в самом низу списка, добавляем небольшой дополнительный отступ
            // чтобы последний элемент был полностью виден
            if (newScrollOffset >= maxScrollOffset) {
                // Добавляем небольшой буфер для последнего элемента
                maxScrollOffset += 1; // Добавляем дополнительную единицу для отступа
            }
            
            newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
            
            // Применяем новое значение прокрутки
            if (newScrollOffset != scrollOffset) {
                scrollOffset = newScrollOffset;
                return true;
            }
            
            return false;
        }
    }
} 