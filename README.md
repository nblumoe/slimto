# slimto

An open HTML5 mobile-first application exploring ideas around how to facilitate
achieving weight loss goals via social collaboration.

## Config

- update `manifest.webapp` as needed (e.g. author)
- update firebase API URL in `config.cljs.example` and rename to `config.cljs`
- at the current stage of development, users need to be created on firebase
  manually

## Development

The application uses the Clojure(script) build tool
[boot](https://github.com/boot-clj/boot).

- start dev server: `boot dev`

## Deployment

By default the deployment is done with [divshot](https://divshot.com/).

- deploy to divshot: `divshot push`
