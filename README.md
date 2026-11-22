# SafeInteract

SafeInteract helps prevent accidental interactions and worn-out tools while building and playing.

It is a client-side Fabric mod; the server does not need to install it.

## Features

- **Build Guard:** after the use button has been held longer than the configured threshold, or after a successful block placement starts the current use sequence, continued right-clicking cannot activate interactive blocks. A quick click behaves normally, and sneaking bypasses this guard. The guard also covers item frames, armor stands, villagers and wandering traders, minecarts, and boats. The block or entity targeted when Smart Hold RMB was enabled is exempt from Build Guard, but not from durability protection.
- **Entity Pierce:** hold Left Alt to target blocks behind item frames, armor stands, villagers and wandering traders, minecarts, boats, or paintings. Reach stays vanilla and player rotation is unchanged.
- **Smart Hold RMB/LMB:** toggle a held use or attack button without generating synthetic clicks. Only one mode can be active at a time. Depending on the enabled guards, Smart Hold stops when:
  - the held item's remaining durability is at or below the threshold (RMB also checks the offhand);
  - there are no empty inventory slots (enabled by default for RMB, disabled by default for LMB);
  - you take damage or lose health;
  - the selected hotbar slot or main-hand item changes;
  - you move farther than the configured tolerance from the starting position;
  - the initially targeted block remains air longer than the configured delay;
  - you die or the player or world changes;
  - a screen other than ordinary chat opens. Chat pauses holding until it closes.

  Disconnecting or disabling the mod also stops holding.
- **Durability guard:** with the default threshold of 5 remaining durability (configurable from 1 to 50), protect axe, hoe, shovel, shears, flint and steel, and brush actions on blocks and entities. On interactive blocks, the block's normal use takes priority unless you sneak. The mining/attack guard also protects low-durability tools and weapons on LMB.
- **HUD and feedback:** an amber shield pulse marks a blocked interaction or durability guard, a cyan shield shows Entity Pierce, and an emerald or red ring shows Smart Hold active or aborted. Action-bar messages have a SafeInteract badge, and events can play quiet vanilla sounds. Indicators, messages, and sounds can be toggled in settings.

## Keybinds

All bindings can be changed in **Controls → SafeInteract**.

| Action | Default |
| --- | --- |
| Entity Pierce | Left Alt |
| Toggle Smart Hold RMB | R |
| Toggle Smart Hold LMB | G |
| Toggle Build Guard | Unbound |
| Open settings | Unbound |

## Configuration

Open the built-in settings screen with the settings keybind or `/safeinteract`. Mod Menu also opens it when Cloth Config is absent. With Cloth Config installed, Mod Menu opens the full settings screen, and the built-in screen has an **Advanced settings…** button.

Use `/safeinteract reload` to reload `config/safeinteract.json`. The full settings include custom always-guard and never-guard block ID lists; the never-guard list takes precedence.

## Multiplayer and anti-cheat

Everything runs on the client. Blocked interactions, digging, and attacks are not sent to the server; vanilla swing behavior can still occur. SafeInteract sends no extra packets, does not extend reach, and does not change rotation. Its client commands are not sent to the server.

Smart Hold automates holding a mouse button. Check the server's rules on AFK play and automation before using it.

## Requirements

- Minecraft 1.21.4
- Fabric Loader 0.16.9 or newer
- Fabric API
- Optional: Mod Menu and Cloth Config

## Building

Use JDK 21 and run:

```sh
./gradlew build
```

The mod jar is written to `build/libs/safeinteract-<version>.jar`.

## License

MIT © 2026 winax1337. See [LICENSE](LICENSE).

## Русский

SafeInteract помогает избежать случайных взаимодействий и поломки инструментов во время строительства и игры.

Это клиентский мод для Fabric; устанавливать его на сервер не нужно.

### Возможности

- **Защита строительства:** после удержания кнопки использования дольше заданного порога или после успешной установки блока в текущей последовательности дальнейшее нажатие ПКМ не активирует интерактивные блоки. Короткое нажатие работает как обычно, а приседание обходит эту защиту. Она также распространяется на рамки, стойки для брони, жителей и странствующих торговцев, вагонетки и лодки. Блок или сущность, на которые был наведён прицел при включении умного удержания ПКМ, исключаются из защиты строительства, но не из защиты прочности.
- **Взаимодействие сквозь сущности:** удерживайте левый Alt, чтобы наводиться на блоки за рамками, стойками для брони, жителями и странствующими торговцами, вагонетками, лодками и картинами. Дальность остаётся стандартной, поворот игрока не изменяется.
- **Умное удержание ПКМ/ЛКМ:** включает удержание кнопки использования или атаки без создания искусственных нажатий. Одновременно может быть активен только один режим. В зависимости от включённых защит удержание прекращается, когда:
  - оставшаяся прочность предмета в руке достигает порога или становится ниже него (для ПКМ также проверяется вторая рука);
  - в инвентаре нет пустых слотов (по умолчанию проверка включена для ПКМ и выключена для ЛКМ);
  - игрок получает урон или теряет здоровье;
  - меняется выбранный слот быстрого доступа или предмет в основной руке;
  - игрок отходит от начальной позиции дальше допустимого расстояния;
  - на месте первоначально выбранного блока воздух остаётся дольше заданной задержки;
  - игрок погибает либо меняется игрок или мир;
  - открывается любой экран, кроме обычного чата. Пока чат открыт, удержание приостанавливается.

  При отключении от сервера или выключении мода удержание также прекращается.
- **Защита прочности:** при стандартном пороге в 5 единиц прочности (настраивается от 1 до 50) защищаются действия топором, мотыгой, лопатой, ножницами, огнивом и кистью по блокам и сущностям. На интерактивных блоках приоритет имеет обычное действие блока, если игрок не приседает. Защита добычи и атаки также предотвращает использование инструментов и оружия с низкой прочностью при нажатии ЛКМ.
- **Индикаторы и сигналы:** янтарная вспышка щита означает заблокированное взаимодействие или срабатывание защиты прочности, голубой щит — активное взаимодействие сквозь сущности, а зелёное или красное кольцо — активное или остановленное умное удержание. Сообщения над панелью быстрого доступа имеют значок SafeInteract; события также могут сопровождаться тихими стандартными звуками. Индикаторы, сообщения и звуки можно отключить в настройках.

### Управление

Все клавиши можно изменить в разделе **Управление → SafeInteract**.

| Действие | По умолчанию |
| --- | --- |
| Взаимодействие сквозь сущности | Левый Alt |
| Умное удержание ПКМ | R |
| Умное удержание ЛКМ | G |
| Переключить защиту строительства | Не назначено |
| Открыть настройки | Не назначено |

### Настройки

Откройте встроенный экран настроек назначенной клавишей или командой `/safeinteract`. Если Cloth Config не установлен, Mod Menu тоже открывает встроенный экран. При наличии Cloth Config в Mod Menu открывается полный экран настроек, а на встроенном экране появляется кнопка **Расширенные настройки…**.

Команда `/safeinteract reload` перечитывает файл `config/safeinteract.json`. В полных настройках доступны списки ID блоков, которые нужно всегда защищать или никогда не защищать; список исключений имеет приоритет.

### Сетевая игра и античит

Все действия мода выполняются на клиенте. Заблокированные взаимодействия, добыча и атаки не отправляются на сервер; стандартный взмах при этом всё ещё возможен. SafeInteract не отправляет дополнительные пакеты, не увеличивает дальность и не меняет поворот игрока. Клиентские команды не отправляются на сервер.

Умное удержание автоматизирует удержание кнопки мыши. Перед использованием ознакомьтесь с правилами сервера об AFK и автоматизации.

### Требования

- Minecraft 1.21.4
- Fabric Loader 0.16.9 или новее
- Fabric API
- Необязательно: Mod Menu и Cloth Config

### Сборка

Используйте JDK 21 и выполните:

```sh
./gradlew build
```

JAR-файл мода будет создан по пути `build/libs/safeinteract-<version>.jar`.

### Лицензия

MIT © 2026 winax1337. См. [LICENSE](LICENSE).
