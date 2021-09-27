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
