const del = require('del');
const fs = require('fs');
const gulp = require('gulp');
const replace = require('gulp-replace');
const rev = require('gulp-rev');
const revRewrite = require('gulp-rev-rewrite');

gulp.task('clean', () => {
  return del(['./target/dist']);
});

gulp.task('server', () => {
  return gulp.src([
      'package.json',
      'package-lock.json',
      'target/release/server.js'])
    .pipe(gulp.dest('target/dist'));
});

gulp.task('static', () => {
  return gulp.src([
      'resources/public/**',
      '!resources/public/css/**'])
    .pipe(gulp.dest('target/dist/resources/public'));
});

gulp.task('css', () => {
  return gulp.src(
      'resources/public/css/*.css',
      { base: 'resources/public' })
    .pipe(rev())
    .pipe(gulp.dest('target/dist/resources/public'))
    .pipe(rev.manifest(
      'target/dist/manifest.json',
      {
        base: 'target/dist',
        merge: true
      }))
    .pipe(gulp.dest('target/dist'));
});

gulp.task('js', () => {
  return gulp.src('target/release/public/**/*.js')
    .pipe(rev())
    .pipe(gulp.dest('target/dist/resources/public'))
    .pipe(rev.manifest(
      'target/dist/manifest.json',
      {
        base: 'target/dist',
        merge: true
      }))
    .pipe(gulp.dest('target/dist'));
});

gulp.task('app', () => {
  const manifest = gulp.src('target/dist/manifest.json');

  return gulp.src([
      'resources/public/index.html'])
    .pipe(revRewrite({
      manifest: manifest,
      replaceInExtensions: ['.html', '.json']
    }))
    .pipe(gulp.dest('target/dist/resources/public'));
});

gulp.task('default', gulp.series('clean', 'server', 'static', 'css', 'js', 'app'));
