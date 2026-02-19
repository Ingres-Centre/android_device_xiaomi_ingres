use std::fmt::{Display, Formatter};

#[derive(Debug, Clone, Default)]
pub struct IncrementalCounter<T>
where
    T: Default + std::ops::AddAssign<i32> + std::fmt::Display + std::fmt::Debug + Copy,
{
    inner: T,
}

impl<T> IncrementalCounter<T>
where
    T: Default + std::ops::AddAssign<i32> + std::fmt::Display + std::fmt::Debug + Copy,
{
    pub fn new(inner: T) -> Self {
        Self { inner }
    }

    pub fn next(&mut self) -> T {
        let current = self.inner;
        self.inner += 1;
        current
    }
}

impl<T> Display for IncrementalCounter<T>
where
    T: Default + std::ops::AddAssign<i32> + std::fmt::Display + std::fmt::Debug + Copy,
{
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.inner)
    }
}
