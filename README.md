# Hanabi Hint Tracker

An offline Android companion for tracking hidden-card information in Hanabi. It supports 4- or 5-card hands, color and number hints, card reordering, play-and-replace, persisted state, and configurable replacement direction.

## Presets

- Standard Hanabi
- Rainbow as a sixth color
- Rainbow as a multicolor suit
- Black Powder (standard suits plus black)

The rules layer is data-driven so expansion definitions can be updated independently of the UI. The app does not identify the physical cards; it records what each card can still be.

## Build

Open the project in Android Studio and run the `app` configuration. GitHub Actions builds a debug APK on pushes and pull requests, and attaches an APK to releases created from `v*` tags.
