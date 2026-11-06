# Sound Files

Place the following sound files in `app/src/main/res/raw/`:

## Required Files

| File | Description | Suggested Duration |
|------|-------------|-------------------|
| `dice_roll.mp3` | Dice rolling sound | 0.5-1s |
| `token_move.mp3` | Token moving on board | 0.2-0.3s |
| `token_capture.mp3` | Capturing opponent's token | 0.5s |
| `token_home.mp3` | Token reaching home/finish | 1s |
| `win.mp3` | Victory celebration | 2-3s |
| `turn_start.mp3` | Notification for your turn | 0.3s |
| `button_click.mp3` | UI button click | 0.1s |
| `error.mp3` | Error/invalid action | 0.3s |
| `player_join.mp3` | Player joining room | 0.5s |
| `chat_message.mp3` | New chat message | 0.2s |

## Free Sound Resources

You can find free sound effects at:
- [Freesound.org](https://freesound.org/)
- [Mixkit](https://mixkit.co/free-sound-effects/)
- [Zapsplat](https://www.zapsplat.com/)
- [Pixabay](https://pixabay.com/sound-effects/)

## Format Requirements

- Format: MP3 or OGG (preferred for Android)
- Sample Rate: 44100 Hz
- Bit Rate: 128 kbps minimum
- Channels: Mono or Stereo

## Placeholder

If you don't have sound files yet, the app will work without them - sounds will simply be disabled until the files are added.

To disable sounds temporarily, you can also call:
```kotlin
soundManager.setSoundEnabled(false)
```
