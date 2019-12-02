# Kubernetes Operators with GraalVM

The repository contains different K8s operator implementations in Java using GraalVM.

## GraalVM installation

I used Jabba as a JVM manager to install GraalVM R14 on my machine. Once this is
done, make sure to install R, Python and Ruby using the Graal updater.

```
# use Jabba to install GraalVM
jabba ls-remote
jabba install graalvm@19.2.1
jabba use graalvm@19.2.1

export GRAALVM_HOME=$JAVA_HOME
```



## Maintainer

M.-Leander Reimer (@lreimer), <mario-leander.reimer@qaware.de>

## License

This software is provided under the MIT open source license, read the `LICENSE`
file for details.
