# jenkins-library

A library for all reform projects to use helping with [shared jenkins code](https://jenkins.io/doc/book/pipeline/shared-libraries/#loading-resources).

## Packaging applications
Applications are packaged using a Groovy class called [Packager](https://git.reform.hmcts.net/reform/jenkins-library/blob/master/src/uk/gov/hmcts/Packager.groovy) into an RPM, it currently supports node and java packages, but can support additional languages as needed by providing a `package.sh` script with [fpm](https://github.com/jordansissel/fpm/) configuration.

### Node specific - Production dependencies
Artifacts should only be built once and go through the pipeline to production.
Before packaging an artifact for deployment all dependencies are available.
This is when any unit testing / other testing that requires those `devDependencies` are done.

When an artifact is packaged it runs `yarn install --production` this means only dependencies that are actually needed are installed on the server.
This is standard practice when working with npm / yarn.
It reduced SSCS's RPM size from ~350mb to ~30MB.

Note that you cannot include the dependencies in one environment and not the other as that violates build once and deploy to multiple environments. hence doing that would mean you would never test your production binary anywhere except production.

Tools like `mocha` should not be available on servers as that's not where the testing should take place.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.txt) file for details

