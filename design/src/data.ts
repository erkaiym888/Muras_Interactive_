export const photos = {
  hero: 'https://images.unsplash.com/photo-1792922386215-9fec611e3e42?auto=format&fit=crop&w=1800&q=88',
  valley: 'https://images.unsplash.com/photo-1792922386261-5952ec998715?auto=format&fit=crop&w=1400&q=85',
  yurt: 'https://images.unsplash.com/photo-1604877249407-f4dce6f21d02?auto=format&fit=crop&w=1200&q=85',
  interior: 'https://images.unsplash.com/photo-1489421382202-f7ec0cfd96f7?auto=format&fit=crop&w=1200&q=85',
  people: 'https://images.unsplash.com/photo-1621425022500-d801b0be1a05?auto=format&fit=crop&w=1200&q=85',
}

export const yurtParts = {
  tunduk: {
    name: 'Түндүк',
    eyebrow: 'Сердце юрты',
    description: 'Круглая деревянная решётка в вершине купола, соединяющая ууки и открывающая юрту небу.',
    purpose: 'Скрепляет кровлю, пропускает дневной свет и выводит дым от очага.',
    meaning: 'Түндүк — символ дома, рода и непрерывности жизни. Его образ находится в центре флага Кыргызской Республики.',
    related: 'Уук · Купол · Очаг',
    image: photos.yurt,
  },
  uuk: {
    name: 'Уук',
    eyebrow: 'Купольные жерди',
    description: 'Изогнутые жерди, образующие купол юрты и связывающие кереге с түндүк.',
    purpose: 'Равномерно распределяют вес войлочного покрытия.',
    meaning: 'Ритм ууков создаёт ощущение порядка и единства внутреннего пространства.',
    related: 'Түндүк · Кереге · Туурдук',
    image: photos.valley,
  },
  kerege: {
    name: 'Кереге',
    eyebrow: 'Стены жилища',
    description: 'Раздвижная деревянная решётка, формирующая круглые стены юрты.',
    purpose: 'Даёт конструкции прочность, лёгкость и возможность быстро собрать жилище.',
    meaning: 'Круг кереге воплощает защищённость семьи и открытость гостю.',
    related: 'Уук · Босого · Канат',
    image: photos.interior,
  },
  eshik: {
    name: 'Эшик',
    eyebrow: 'Вход в боз үй',
    description: 'Традиционный дверной блок с деревянной рамой, порогом и двустворчатой резной дверью.',
    purpose: 'Укрепляет разрыв в кольце кереге и образует защищённый вход в жилище.',
    meaning: 'Порог юрты воспринимается как граница домашнего пространства; входить принято с уважением, не наступая на него.',
    related: 'Босого · Кереге · Туурдук',
    image: photos.yurt,
  },
  shyrdak: {
    name: 'Шырдак',
    eyebrow: 'Войлочный ковёр',
    description: 'Традиционный ковёр из цветного войлока с орнаментом, созданным в технике мозаики.',
    purpose: 'Утепляет пол и украшает почётную часть юрты.',
    meaning: 'Орнаменты передают пожелания благополучия, защиты и продолжения рода.',
    related: 'Ала кийиз · Оймо · Төр',
    image: photos.people,
  },
}

export const traditions = [
  { id: 'beshik', title: 'Бешик той', category: 'Рождение ребёнка', description: 'Семейный обряд первого укладывания младенца в традиционную колыбель — бешик.', image: photos.interior },
  { id: 'tushoo', title: 'Тушоо кесүү', category: 'Рождение ребёнка', description: 'Праздник первых шагов ребёнка и символическое разрезание пут.', image: photos.people },
  { id: 'kyz', title: 'Кыз узатуу', category: 'Свадебные', description: 'Торжественные проводы невесты из родительского дома с благословениями.', image: photos.hero },
  { id: 'bata', title: 'Бата берүү', category: 'Бытовые', description: 'Устное благословение старших, произносимое перед важным начинанием.', image: photos.valley },
]
