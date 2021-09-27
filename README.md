# `scala_restructure` CLI tool

Similar to `scalafix`, except that this is more about moving files around, rather than just modifying the contents of a file. Available rules are:

* Move files into directories that match their packages (i.e. Java-style)
* Split and move files so that their filename matches the top-level type (i.e. Java-style)

*Note: `scalafix` may actually have support for this and I just don't know about it yet. Who knows. If nothing else, at least the rule logic is worked out here.*


# Usage

```
scala_restructure v0.1.0-SNAPSHOT

Usage: scala_restructure [options] [<dir | glob of dirs>...]

  <dir | glob of dirs>...  Source directory roots. Default: **/src/*/scala*
  -d, --align-dirs         Move files into directories that match their packages (i.e. Java-style) (on by default)
  -f, --align-files        Split and move files so that their filename matches the top-level type (i.e. Java-style) (on by default)
  -i, --ignore-errors      Ignore (ignorable) errors
  -n, --dry-run            Don't actually modify the file system
  -v, --verbose            Print more information. Useful for debugging and seeing what's going on under the hood.
  -h, --help               Prints this usage text
```

# Running locally

1. Build via `sbt assembly`
2. Run via `./run`
