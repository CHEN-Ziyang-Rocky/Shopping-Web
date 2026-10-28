'use client'; // 这是一个客户端交互组件
import { useState, useEffect } from 'react';
import { Box, Typography, IconButton } from '@mui/material';
import ArrowBackIosNewIcon from '@mui/icons-material/ArrowBackIosNew';
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';

const slides = [
  {
    url: '/banners/End-of-season-sale.png', // 直接使用绝对路径指向 public 文件夹
    title: 'Everything you want to buy is available here',
    subtitle: 'Up to 60% OFF End of Season Sale'
  },
  {
    url: '/banners/slide1.jpg',
    title: 'Secure & Fast',
    subtitle: 'SALE SPECIAL OFFER 30% OFF'
  },
  {
    url: '/banners/slide2.jpeg',
    title: 'Jewellery Days',
    subtitle: 'SALE SPECIAL OFFER 50% OFF'
  },
];

export default function HeroSlider() {
  const [currentIndex, setCurrentIndex] = useState(0);

  // 自动播放逻辑 (Optional)
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentIndex(index => (index + 1) % slides.length);
    }, 5000); // 5秒切换
    return () => clearInterval(timer);
  }, []);

  const prevSlide = () => {
    const isFirstSlide = currentIndex === 0;
    const newIndex = isFirstSlide ? slides.length - 1 : currentIndex - 1;
    setCurrentIndex(newIndex);
  };

  const nextSlide = () => {
    const isLastSlide = currentIndex === slides.length - 1;
    const newIndex = isLastSlide ? 0 : currentIndex + 1;
    setCurrentIndex(newIndex);
  };

  return (
    <Box sx={{
      width: '100%',
      height: { xs: '300px', md: '500px' }, // 响应式高度
      position: 'relative',
      overflow: 'hidden'
    }}>
      {/* 背景图层 */}
      <div style={{
          width: '100%',
          height: '100%',
          backgroundImage: `url(${slides[currentIndex].url})`,
          backgroundPosition: 'center',
          backgroundSize: 'cover',
          transition: 'background-image 0.5s ease-in-out'
        }}
      />

      {/* 遮罩层 (让文字更清晰) */}
      <Box sx={{
        position: 'absolute', top: 0, left: 0, width: '100%', height: '100%',
        backgroundColor: 'rgba(0,0,0,0.4)', display: 'flex', flexDirection: 'column',
        justifyContent: 'center', alignItems: 'center', color: 'white', textAlign: 'center'
      }}>
        <Typography variant="h2" sx={{ fontWeight: 'bold', fontSize: { xs: '2rem', md: '4rem' } }}>
          {slides[currentIndex].title}
        </Typography>
        <Typography variant="h5" sx={{ mt: 2 }}>
          {slides[currentIndex].subtitle}
        </Typography>
      </Box>

      {/* 左右箭头 */}
      <IconButton
        onClick={prevSlide}
        sx={{ position: 'absolute', top: '50%', left: '20px', color: 'white', bgcolor: 'rgba(0,0,0,0.5)', '&:hover': { bgcolor: 'rgba(0,0,0,0.8)' } }}
      >
        <ArrowBackIosNewIcon />
      </IconButton>
      <IconButton
        onClick={nextSlide}
        sx={{ position: 'absolute', top: '50%', right: '20px', color: 'white', bgcolor: 'rgba(0,0,0,0.5)', '&:hover': { bgcolor: 'rgba(0,0,0,0.8)' } }}
      >
        <ArrowForwardIosIcon />
      </IconButton>
    </Box>
  );
}
