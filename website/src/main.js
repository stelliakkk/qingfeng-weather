const header = document.querySelector('.site-header');
const updateHeader = () => header.classList.toggle('scrolled', window.scrollY > 24);
window.addEventListener('scroll', updateHeader, { passive: true });
updateHeader();

const scene = document.querySelector('#experience');
const photos = scene.querySelector('.weather-photos');
const message = scene.querySelector('.weather-message');
const options = scene.querySelectorAll('[data-mode]');
const descriptions = {
  sun: '晴 · 暖光穿过轻云，在湖面铺开。',
  cloud: '阴 · 云层压低，湖畔落下均匀的柔光。',
  rain: '雨 · 细雨掠过林梢，湖面泛起涟漪。',
  fog: '雾 · 水汽缓缓流过，远山隐入浅雾。',
};
const base = import.meta.env.BASE_URL;
const loadedPhotos = new Map();
let requested = 'sun';

function loadPhoto(mode) {
  if (loadedPhotos.has(mode)) return loadedPhotos.get(mode);

  const picture = document.createElement('picture');
  const source = document.createElement('source');
  source.media = '(max-width: 767px)';
  source.srcset = base + 'media/' + mode + '-mobile.webp';
  const image = document.createElement('img');
  image.alt = '';
  picture.className = 'backdrop photo';
  picture.dataset.photo = mode;
  picture.append(source, image);
  photos.append(picture);
  image.src = base + 'media/' + mode + '.webp';

  const ready = image.decode().then(() => picture).catch(error => {
    picture.remove();
    loadedPhotos.delete(mode);
    throw error;
  });
  loadedPhotos.set(mode, ready);
  return ready;
}

async function showWeather(mode) {
  requested = mode;
  if (mode === scene.dataset.weather) {
    message.textContent = descriptions[mode];
    return;
  }
  message.textContent = '正在加载场景…';
  try {
    const next = mode === 'sun' ? photos.querySelector('[data-photo="sun"]') : await loadPhoto(mode);
    if (requested !== mode) return;
    const previous = photos.querySelector('.photo.active');
    photos.querySelector('.photo.underneath')?.classList.remove('underneath');
    previous.classList.replace('active', 'underneath');
    next.classList.add('active');
    scene.dataset.weather = mode;
    options.forEach(button => button.setAttribute('aria-pressed', String(button.dataset.mode === mode)));
    message.textContent = descriptions[mode];
  } catch {
    if (requested === mode) message.textContent = '暂时无法加载场景，请重试。';
  }
}

options.forEach(button => button.addEventListener('click', () => showWeather(button.dataset.mode)));
const rain = scene.querySelector('.rain');
for (let index = 0; index < 32; index += 1) {
  const drop = document.createElement('i');
  drop.style.left = (index * 2.9 + (index * 13 % 7) * .5) + '%';
  drop.style.setProperty('--duration', (1.15 + index % 9 * .12) + 's');
  drop.style.setProperty('--delay', (index * -0.17) + 's');
  rain.append(drop);
}
new IntersectionObserver(([entry]) => scene.classList.toggle('is-visible', entry.isIntersecting)).observe(scene);
new ResizeObserver(() => rain.style.setProperty('--fall-distance', (scene.clientHeight * 1.15) + 'px')).observe(scene);

const lightbox = document.querySelector('.lightbox');
const enlarged = lightbox.querySelector('img');
const lightboxTitle = lightbox.querySelector('h2');

document.querySelectorAll('.shot').forEach(link => {
  link.addEventListener('click', event => {
    if (!lightbox.showModal || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    const caption = link.parentElement.querySelector('figcaption strong');
    lightboxTitle.textContent = caption.textContent;
    enlarged.alt = link.querySelector('img').alt;
    enlarged.src = link.href;
    link.focus({ preventScroll: true });
    lightbox.showModal();
  });
});

lightbox.addEventListener('click', event => {
  if (event.target !== lightbox) return;
  const bounds = lightbox.getBoundingClientRect();
  if (event.clientX < bounds.left || event.clientX > bounds.right
    || event.clientY < bounds.top || event.clientY > bounds.bottom) lightbox.close();
});
